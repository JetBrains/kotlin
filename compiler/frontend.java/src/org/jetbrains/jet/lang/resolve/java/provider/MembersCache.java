/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.resolve.java.provider;

import com.google.common.collect.ImmutableSet;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiFormatUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.resolve.java.*;
import org.jetbrains.jet.lang.resolve.java.kt.JetClassAnnotation;
import org.jetbrains.jet.lang.resolve.java.prop.PropertyNameUtils;
import org.jetbrains.jet.lang.resolve.java.prop.PropertyParseResult;
import org.jetbrains.jet.lang.resolve.java.wrapper.*;
import org.jetbrains.jet.lang.resolve.name.Name;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.intellij.psi.util.PsiFormatUtilBase.*;

public final class MembersCache {
    private static final ImmutableSet<String> OBJECT_METHODS = ImmutableSet.of("hashCode()", "equals(java.lang.Object)", "toString()");

    @NotNull
    private final Map<Name, NamedMembers> namedMembersMap = new HashMap<Name, NamedMembers>();

    @Nullable
    public NamedMembers get(@NotNull Name name) {
        return namedMembersMap.get(name);
    }

    @NotNull
    public Collection<NamedMembers> allMembers() {
        return namedMembersMap.values();
    }

    @NotNull
    private NamedMembers getOrCreateEmpty(@NotNull Name name) {
        NamedMembers r = namedMembersMap.get(name);
        if (r == null) {
            r = new NamedMembers(name);
            namedMembersMap.put(name, r);
        }
        return r;
    }

    @NotNull
    public static MembersCache buildMembersByNameCache(
            @NotNull MembersCache membersCache,
            @NotNull PsiClassFinder finder,
            @Nullable PsiClass psiClass,
            @Nullable PsiPackage psiPackage,
            boolean staticMembers,
            boolean isKotlin
    ) {
        if (psiClass != null) {
            membersCache.new ClassMemberProcessor(new PsiClassWrapper(psiClass), staticMembers, isKotlin).process();
        }

        //TODO:
        List<PsiClass> classes = psiPackage != null ? finder.findPsiClasses(psiPackage) : finder.findInnerPsiClasses(psiClass);
        membersCache.new ExtraPackageMembersProcessor(classes).process();
        return membersCache;
    }

    private class ExtraPackageMembersProcessor { // 'extra' means that PSI elements for these members are not just top-level classes
        @NotNull
        private final List<PsiClass> psiClasses;

        private ExtraPackageMembersProcessor(@NotNull List<PsiClass> classes) {
            psiClasses = classes;
        }

        private void process() {
            for (PsiClass psiClass : psiClasses) {
                if (!(psiClass instanceof JetJavaMirrorMarker)) { // to filter out JetLightClasses
                    if (JetClassAnnotation.get(psiClass).kind() == JvmStdlibNames.FLAG_CLASS_KIND_OBJECT) {
                        processObjectClass(psiClass);
                    }
                    if (!DescriptorResolverUtils.isKotlinClass(psiClass) && isSamInterface(psiClass)) {
                        processSamInterface(psiClass);
                    }
                }
            }
        }

        private void processObjectClass(@NotNull PsiClass psiClass) {
            PsiField instanceField = psiClass.findFieldByName(JvmAbi.INSTANCE_FIELD, false);
            if (instanceField != null) {
                NamedMembers namedMembers = getOrCreateEmpty(Name.identifier(psiClass.getName()));

                TypeSource type = new TypeSource("", instanceField.getType(), instanceField);
                namedMembers.addPropertyAccessor(new PropertyPsiDataElement(new PsiFieldWrapper(instanceField), type, null));
            }
        }

        private void processSamInterface(@NotNull PsiClass psiClass) {
            NamedMembers namedMembers = getOrCreateEmpty(Name.identifier(psiClass.getName()));
            namedMembers.setSamInterface(psiClass);
        }
    }

    private class ClassMemberProcessor {
        @NotNull
        private final PsiClassWrapper psiClass;
        private final boolean staticMembers;
        private final boolean kotlin;

        private ClassMemberProcessor(@NotNull PsiClassWrapper psiClass, boolean staticMembers, boolean kotlin) {
            this.psiClass = psiClass;
            this.staticMembers = staticMembers;
            this.kotlin = kotlin;
        }

        public void process() {
            processFields();
            processMethods();
            processNestedClasses();
        }

        private boolean includeMember(PsiMemberWrapper member) {
            if (psiClass.getPsiClass().isEnum() && staticMembers) {
                return member.isStatic();
            }

            if (member.isStatic() != staticMembers) {
                return false;
            }

            if (member.getPsiMember().getContainingClass() != psiClass.getPsiClass()) {
                return false;
            }

            //process private accessors
            if (member.isPrivate()
                && !(member instanceof PsiMethodWrapper && ((PsiMethodWrapper)member).getJetMethodAnnotation().hasPropertyFlag())) {
                return false;
            }

            if (isObjectMethodInInterface(member.getPsiMember())) {
                return false;
            }

            return true;
        }

        private void processFields() {
            // Hack to load static members for enum class loaded from class file
            if (kotlin && !psiClass.getPsiClass().isEnum()) {
                return;
            }
            for (PsiField field : psiClass.getPsiClass().getAllFields()) {
                PsiFieldWrapper fieldWrapper = new PsiFieldWrapper(field);

                // group must be created even for excluded field
                NamedMembers namedMembers = getOrCreateEmpty(Name.identifier(fieldWrapper.getName()));

                if (!includeMember(fieldWrapper)) {
                    continue;
                }

                TypeSource type = new TypeSource("", fieldWrapper.getType(), field);
                namedMembers.addPropertyAccessor(new PropertyPsiDataElement(fieldWrapper, type, null));
            }
        }

        private void processMethods() {
            parseAllMethodsAsProperties();
            processOwnMethods();
        }

        private void processOwnMethods() {
            for (PsiMethod ownMethod : psiClass.getPsiClass().getMethods()) {
                PsiMethodWrapper method = new PsiMethodWrapper(ownMethod);

                if (!includeMember(method)) {
                    continue;
                }

                PropertyParseResult propertyParseResult = PropertyNameUtils.parseMethodToProperty(method.getName());

                // TODO: remove getJavaClass
                if (propertyParseResult != null && propertyParseResult.isGetter()) {
                    processGetter(ownMethod, method, propertyParseResult);
                }
                else if (propertyParseResult != null && !propertyParseResult.isGetter()) {
                    processSetter(method, propertyParseResult);
                }

                if (!method.getJetMethodAnnotation().hasPropertyFlag()) {
                    NamedMembers namedMembers = getOrCreateEmpty(Name.identifier(method.getName()));
                    namedMembers.addMethod(method);
                }
            }
        }

        private void processSetter(PsiMethodWrapper method, PropertyParseResult propertyParseResult) {
            String propertyName = propertyParseResult.getPropertyName();
            NamedMembers members = getOrCreateEmpty(Name.identifier(propertyName));

            if (method.getJetMethodAnnotation().hasPropertyFlag()) {
                if (method.getParameters().size() == 0) {
                    // TODO: report error properly
                    throw new IllegalStateException();
                }

                int i = 0;

                TypeSource receiverType = null;
                PsiParameterWrapper p1 = method.getParameter(0);
                if (p1.getJetValueParameter().receiver()) {
                    receiverType = new TypeSource(p1.getJetValueParameter().type(), p1.getPsiParameter().getType(), p1.getPsiParameter());
                    ++i;
                }

                while (i < method.getParameters().size() && method.getParameter(i).getJetTypeParameter().isDefined()) {
                    ++i;
                }

                if (i + 1 != method.getParameters().size()) {
                    throw new IllegalStateException();
                }

                PsiParameterWrapper propertyTypeParameter = method.getParameter(i);
                TypeSource propertyType =
                        new TypeSource(method.getJetMethodAnnotation().propertyType(), propertyTypeParameter.getPsiParameter().getType(),
                                       propertyTypeParameter.getPsiParameter());

                members.addPropertyAccessor(new PropertyPsiDataElement(method, false, propertyType, receiverType));
            }
        }

        private void processGetter(PsiMethod ownMethod, PsiMethodWrapper method, PropertyParseResult propertyParseResult) {
            String propertyName = propertyParseResult.getPropertyName();
            NamedMembers members = getOrCreateEmpty(Name.identifier(propertyName));

            // TODO: some java properties too
            if (method.getJetMethodAnnotation().hasPropertyFlag()) {

                int i = 0;

                TypeSource receiverType;
                if (i < method.getParameters().size() && method.getParameter(i).getJetValueParameter().receiver()) {
                    PsiParameterWrapper receiverParameter = method.getParameter(i);
                    receiverType =
                            new TypeSource(receiverParameter.getJetValueParameter().type(), receiverParameter.getPsiParameter().getType(),
                                           receiverParameter.getPsiParameter());
                    ++i;
                }
                else {
                    receiverType = null;
                }

                while (i < method.getParameters().size() && method.getParameter(i).getJetTypeParameter().isDefined()) {
                    // TODO: store is reified
                    ++i;
                }

                if (i != method.getParameters().size()) {
                    // TODO: report error properly
                    throw new IllegalStateException("something is wrong with method " + ownMethod);
                }

                // TODO: what if returnType == null?
                PsiType returnType = method.getReturnType();
                assert returnType != null;
                TypeSource propertyType = new TypeSource(method.getJetMethodAnnotation().propertyType(), returnType, method.getPsiMethod());

                members.addPropertyAccessor(new PropertyPsiDataElement(method, true, propertyType, receiverType));
            }
        }

        private void parseAllMethodsAsProperties() {
            for (PsiMethod method : psiClass.getPsiClass().getAllMethods()) {
                createEmptyEntry(Name.identifier(method.getName()));

                PropertyParseResult propertyParseResult = PropertyNameUtils.parseMethodToProperty(method.getName());
                if (propertyParseResult != null) {
                    createEmptyEntry(Name.identifier(propertyParseResult.getPropertyName()));
                }
            }
        }

        private void createEmptyEntry(@NotNull Name identifier) {
            getOrCreateEmpty(identifier);
        }

        private void processNestedClasses() {
            if (!staticMembers) {
                return;
            }
            for (PsiClass nested : psiClass.getPsiClass().getInnerClasses()) {
                if (isSamInterface(nested)) {
                    NamedMembers namedMembers = getOrCreateEmpty(Name.identifier(nested.getName()));
                    namedMembers.setSamInterface(nested);
                }
            }
        }
    }

    public static boolean isObjectMethodInInterface(@NotNull PsiMember member) {
        if (!(member instanceof PsiMethod)) {
            return false;
        }
        PsiClass containingClass = member.getContainingClass();
        assert containingClass != null : "containing class is null for " + member;

        if (!containingClass.isInterface()) {
            return false;
        }

        return isObjectMethod((PsiMethod) member);
    }

    private static boolean isObjectMethod(PsiMethod method) {
        String formattedMethod = PsiFormatUtil.formatMethod(
                method, PsiSubstitutor.EMPTY, SHOW_NAME | SHOW_PARAMETERS, SHOW_TYPE | SHOW_FQ_CLASS_NAMES);
        return OBJECT_METHODS.contains(formattedMethod);
    }

    public static boolean isSamInterface(@NotNull PsiClass psiClass) {
        if (!psiClass.isInterface()) {
            return false;
        }

        int foundAbstractMethods = 0;
        for (PsiMethod method : psiClass.getAllMethods()) {
            if (!isObjectMethod(method) && method.hasModifierProperty(PsiModifier.ABSTRACT)) {
                foundAbstractMethods++;

                if (method.hasTypeParameters()) {
                    return false;
                }
            }
        }
        return foundAbstractMethods == 1;
    }

}
