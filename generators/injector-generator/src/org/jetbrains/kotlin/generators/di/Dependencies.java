/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.generators.di;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.*;

import static org.jetbrains.kotlin.generators.di.InjectorGeneratorUtil.var;

public class Dependencies {

    private final Set<Field> allFields = Sets.newLinkedHashSet();
    private final Set<Field> satisfied = Sets.newHashSet();
    private final Set<Field> used = Sets.newHashSet();
    private final Multimap<DiType, Field> typeToFields = HashMultimap.create();

    private final Set<Field> newFields = Sets.newLinkedHashSet();

    public void addField(@NotNull Field field) {
        allFields.add(field);
        typeToFields.put(field.getType(), field);
    }

    public void addSatisfiedField(@NotNull Field field) {
        addField(field);
        satisfied.add(field);
    }

    private Field addNewField(@NotNull DiType type) {
        Field field = Field.create(false, type, var(type), null);
        addField(field);
        newFields.add(field);
        return field;
    }

    private void satisfyDependenciesFor(Field field, ImmutableStack<Field> neededFor) {
        if (!satisfied.add(field)) return;

        Expression initialization = field.getInitialization();
        if (initialization instanceof InstantiateType) {
            initializeByConstructorCall(field, neededFor);
        }
        DiType typeToInitialize = InjectorGeneratorUtil.getEffectiveFieldType(field);

        // Sort setters in order to get deterministic behavior
        List<Method> declaredMethods = Lists.newArrayList(typeToInitialize.getClazz().getDeclaredMethods());
        Collections.sort(declaredMethods, new Comparator<Method>() {
            @Override
            public int compare(@NotNull Method o1, @NotNull Method o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        for (Method method : declaredMethods) {
            if (method.getAnnotation(Inject.class) == null
                || !method.getName().startsWith("set")
                || method.getParameterTypes().length != 1) {
                continue;
            }

            Type parameterType = method.getGenericParameterTypes()[0];


            Field dependency = findDependencyOfType(
                    DiType.fromReflectionType(parameterType),
                    field + ": " + method + ": " + allFields,
                    neededFor.prepend(field)
            );
            used.add(dependency);

            field.getDependencies().add(new SetterDependency(field, method.getName(), dependency));
        }
    }

    private Field findDependencyOfType(DiType parameterType, String errorMessage, ImmutableStack<Field> neededFor) {
        List<Field> fields = Lists.newArrayList();
        for (Map.Entry<DiType, Field> entry : typeToFields.entries()) {
            if (parameterType.isAssignableFrom(entry.getKey())) {
                fields.add(entry.getValue());
            }
        }

        if (fields.isEmpty()) {

            if (parameterType.getClazz().isPrimitive() || parameterType.getClazz().getPackage().getName().equals("java.lang")) {
                throw new IllegalArgumentException(
                        "cannot declare magic field of type " + parameterType + ": " + errorMessage);
            }

            Field dependency = addNewField(parameterType);
            satisfyDependenciesFor(dependency, neededFor);
            return dependency;
        }
        else if (fields.size() == 1) {
            return fields.iterator().next();
        }
        else {
            throw new IllegalArgumentException("Ambiguous dependency: \n"
                                               + errorMessage
                                               + "\nneeded for " + neededFor
                                               + "\navailable: " + fields);
        }
    }

    private void initializeByConstructorCall(Field field, ImmutableStack<Field> neededFor) {
        //noinspection RedundantCast
        DiType type = ((InstantiateType) field.getInitialization()).getType();

        Class<?> clazz = type.getClazz();
        if (clazz.isInterface()) {
            if (initializeAsSingleton(field, type)) return;
            throw new IllegalArgumentException("cannot instantiate interface: " + clazz.getName() + " needed for " + neededFor);
        }
        if (Modifier.isAbstract(clazz.getModifiers())) {
            if (initializeAsSingleton(field, type)) return;
            throw new IllegalArgumentException("cannot instantiate abstract class: " + clazz.getName() + " needed for " + neededFor);
        }

        // Note: projections are not computed here

        // Look for constructor
        List<Constructor<?>> publicConstructors = findPublicConstructors(clazz.getConstructors());
        if (publicConstructors.size() != 1) {
            if (initializeAsSingleton(field, type)) return;
            if (publicConstructors.size() == 0) {
                throw new IllegalArgumentException("No public constructor: " + clazz.getName() + " needed for " + neededFor);
            }
            else {
                throw new IllegalArgumentException("Too many public constructors in " + clazz.getName() + " needed for " + neededFor);
            }
        }

        Constructor<?> publicConstructor = publicConstructors.get(0);

        // Find arguments
        ConstructorCall dependency = new ConstructorCall(publicConstructor);
        Type[] parameterTypes = publicConstructor.getGenericParameterTypes();
        try {
            for (Type parameterType : parameterTypes) {
                Field fieldForParameter = findDependencyOfType(
                        DiType.fromReflectionType(parameterType),
                        "constructor: " + publicConstructor + ", parameter: " + parameterType,
                        neededFor.prepend(field)
                );
                used.add(fieldForParameter);
                dependency.getConstructorArguments().add(fieldForParameter);
            }
        }
        catch (InstantiationFailedException e) {
            throw e;
        }
        catch (RuntimeException e) {
            throw new InstantiationFailedException(
                    "Could not instantiate '" + field + "' by calling " + publicConstructor + "\nneeded for " + neededFor,
                    e
            );
        }

        field.setInitialization(dependency);
    }

    @NotNull
    private static List<Constructor<?>> findPublicConstructors(Constructor<?>[] constructors) {
        List<Constructor<?>> result = new ArrayList<Constructor<?>>();
        for (Constructor<?> constructor : constructors) {
            if (Modifier.isPublic(constructor.getModifiers())) {
                result.add(constructor);
            }
        }
        return result;
    }

    private static boolean initializeAsSingleton(Field field, DiType type) {
        Class<?> clazz = type.getClazz();

        return initializeBySingletonMethod(field, clazz, "getInstance")
                || initializeBySingletonField(field, clazz, "INSTANCE")
                || initializeBySingletonField(field, clazz, "INSTANCE$");
    }

    private static boolean initializeBySingletonMethod(Field field, Class<?> clazz, String name) {
        try {
            clazz.getMethod(name);
            field.setInitialization(GetSingleton.byMethod(clazz, name));
            return true;
        }
        catch (NoSuchMethodException e) {
            // Ignored
        }
        return false;
    }

    private static boolean initializeBySingletonField(Field field, Class<?> clazz, String name) {
        try {
            clazz.getField(name);
            field.setInitialization(GetSingleton.byField(clazz, name));
            return true;
        }
        catch (NoSuchFieldException e) {
            // Ignored
        }

        return false;
    }

    public Collection<Field> satisfyDependencies() {
        for (Field field : Lists.newArrayList(allFields)) {
            satisfyDependenciesFor(field, LinkedImmutableStack.<Field>empty());
        }
        return newFields;
    }

    @NotNull
    public Set<Field> getUsedFields() {
        return used;
    }

    private interface ImmutableStack<T> {
        @NotNull
        ImmutableStack<T> prepend(T t);
    }

    private static class LinkedImmutableStack<T> implements ImmutableStack<T> {

        private static final ImmutableStack EMPTY = new ImmutableStack() {
            @NotNull
            @Override
            public ImmutableStack prepend(Object o) {
                return create(o);
            }

            @Override
            public String toString() {
                return "<itself>";
            }
        };

        @NotNull
        @SuppressWarnings("unchecked")
        public static <T> ImmutableStack<T> empty() {
            return EMPTY;
        }

        @NotNull
        public static <T> LinkedImmutableStack<T> create(@NotNull T t) {
            return new LinkedImmutableStack<T>(t, LinkedImmutableStack.<T>empty());
        }

        private final T head;
        private final ImmutableStack<T> tail;

        private LinkedImmutableStack(@NotNull T head, @NotNull ImmutableStack<T> tail) {
            this.head = head;
            this.tail = tail;
        }

        @NotNull
        @Override
        public LinkedImmutableStack<T> prepend(@NotNull T t) {
            return new LinkedImmutableStack<T>(t, this);
        }

        @Override
        public String toString() {
            return doToString(this, new StringBuilder()).toString();
        }

        private static <T> CharSequence doToString(@NotNull ImmutableStack<T> stack, StringBuilder builder) {
            if (stack == empty()) {
                builder.append("|");
                return builder;
            }

            LinkedImmutableStack<T> list = (LinkedImmutableStack<T>) stack;
            builder.append("\n\t").append(list.head).append(" -> ");
            return doToString(list.tail, builder);
        }
    }

}
