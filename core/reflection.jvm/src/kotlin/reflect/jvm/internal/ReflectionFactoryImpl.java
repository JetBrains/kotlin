/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */


package kotlin.reflect.jvm.internal;

import kotlin.Metadata;
import kotlin.collections.CollectionsKt;
import kotlin.jvm.internal.*;
import kotlin.metadata.KmConstructor;
import kotlin.metadata.KmFunction;
import kotlin.metadata.KmProperty;
import kotlin.reflect.*;
import kotlin.reflect.full.KClassifiers;
import kotlin.reflect.jvm.ReflectLambdaKt;
import kotlin.reflect.jvm.internal.types.TypeOfImplKt;
import kotlin.text.MatchResult;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.List;

/**
 * @suppress
 */
@SuppressWarnings({"UnusedDeclaration", "unchecked", "rawtypes"})
public class ReflectionFactoryImpl extends ReflectionFactory {
    @Override
    public KClass createKotlinClass(Class javaClass) {
        return new KClassImpl(javaClass);
    }

    @Override
    public KClass createKotlinClass(Class javaClass, String internalName) {
        return new KClassImpl(javaClass);
    }

    @Override
    public KDeclarationContainer getOrCreateKotlinPackage(Class javaClass, String moduleName) {
        // moduleName is unused deliberately and only left in public ABI
        return CachesKt.getOrCreateKotlinPackage(javaClass);
    }

    @Override
    public KClass getOrCreateKotlinClass(Class javaClass) {
        return CachesKt.getOrCreateKotlinClass(javaClass);
    }

    @Override
    public KClass getOrCreateKotlinClass(Class javaClass, String internalName) {
        return CachesKt.getOrCreateKotlinClass(javaClass);
    }

    @Override
    public String renderLambdaToString(Lambda lambda) {
        return renderLambdaToString((FunctionBase) lambda);
    }

    @Override
    public String renderLambdaToString(FunctionBase lambda) {
        KFunction kFunction = ReflectLambdaKt.reflect(lambda);
        if (kFunction != null) {
            return ReflectionObjectRenderer.INSTANCE.renderLambda(kFunction);
        }
        return super.renderLambdaToString(lambda);
    }

    // Functions

    @Override
    public KFunction function(FunctionReference f) {
        KDeclarationContainerImpl container = getOwner(f);
        String name = f.getName();
        String signature = f.getSignature();
        Object boundReceiver = f.getBoundReceiver();
        if (!SystemPropertiesKt.getUseK1Implementation()) {
            if (name.equals("<init>")) {
                if (isJavaClass(container)) {
                    Constructor<?> constructor = container.findJavaConstructor(signature);
                    return new JavaKConstructor(container, constructor, boundReceiver);
                }
                else {
                    KmConstructor kmConstructor = container.findConstructorMetadata(signature);
                    return new KotlinKConstructor(container, signature, boundReceiver, kmConstructor);
                }
            }
            else if (container instanceof KPackageImpl) {
                KmFunction kmFunction = container.findFunctionMetadata(name, signature);
                return new KotlinKNamedFunction(container, signature, boundReceiver, kmFunction, KCallableOverriddenStorage.EMPTY);
            }
            else if (container instanceof KClassImpl<?> && !((KClassImpl<?>) container).isComplicatedBuiltinSubclass() &&
                     (!SystemPropertiesKt.getUseK1ImplementationForMembers() || isJavaClass(container))) {
                ReflectKFunction result = (ReflectKFunction) CollectionsKt.firstOrNull(
                        ((KClassImpl<?>) container).getData().getValue().getMembersByName(name),
                        it -> it instanceof ReflectKFunction && ((ReflectKFunction) it).getSignature().equals(signature)
                );
                if (result == null) {
                    throw new KotlinReflectionInternalError(
                            "Function '" + name + "' (JVM signature: " + signature + ") not resolved in " + container
                    );
                }
                return (KFunction<?>) result.rebind(boundReceiver);
            }
        }
        return new DescriptorKFunction(container, name, signature, boundReceiver);
    }

    // Properties

    @Override
    public KProperty0 property0(PropertyReference0 p) {
        KDeclarationContainerImpl container = getOwner(p);
        String name = p.getName();
        String signature = p.getSignature();
        Object boundReceiver = p.getBoundReceiver();
        if (!SystemPropertiesKt.getUseK1Implementation()) {
            return new LazyKProperty0(name, () -> {
                MatchResult result = KDeclarationContainerImpl.LOCAL_PROPERTY_SIGNATURE.matchEntire(signature);
                if (result != null) {
                    List<String> values = result.getGroupValues();
                    return container.createLocalProperty(Integer.parseInt(values.get(1)), signature);
                }
                else if (container instanceof KPackageImpl) {
                    KmProperty kmProperty = container.findPropertyMetadata(name, signature);
                    return new KotlinKProperty0(container, signature, boundReceiver, kmProperty, KCallableOverriddenStorage.EMPTY);
                }
                else if (container instanceof KClassImpl && container.getJClass().getAnnotation(Metadata.class) == null) {
                    return findProperty((KClassImpl<?>) container, name, signature, boundReceiver);
                }
                return new DescriptorKProperty0(container, name, signature, boundReceiver);
            });
        }
        return new DescriptorKProperty0(container, name, signature, boundReceiver);
    }

    @Override
    public KMutableProperty0 mutableProperty0(MutablePropertyReference0 p) {
        KDeclarationContainerImpl container = getOwner(p);
        String name = p.getName();
        String signature = p.getSignature();
        Object boundReceiver = p.getBoundReceiver();
        if (!SystemPropertiesKt.getUseK1Implementation()) {
            return new LazyKMutableProperty0(name, () -> {
                MatchResult result = KDeclarationContainerImpl.LOCAL_PROPERTY_SIGNATURE.matchEntire(signature);
                if (result != null) {
                    List<String> values = result.getGroupValues();
                    return container.createLocalProperty(Integer.parseInt(values.get(1)), signature);
                }
                else if (container instanceof KPackageImpl) {
                    KmProperty kmProperty = container.findPropertyMetadata(name, signature);
                    return new KotlinKMutableProperty0(
                            container, signature, boundReceiver, kmProperty, KCallableOverriddenStorage.EMPTY
                    );
                }
                else if (container instanceof KClassImpl && container.getJClass().getAnnotation(Metadata.class) == null) {
                    return findProperty((KClassImpl<?>) container, name, signature, boundReceiver);
                }
                return new DescriptorKMutableProperty0(container, name, signature, boundReceiver);
            });
        }
        return new DescriptorKMutableProperty0(container, name, signature, boundReceiver);
    }

    @Override
    public KProperty1 property1(PropertyReference1 p) {
        KDeclarationContainerImpl container = getOwner(p);
        String name = p.getName();
        String signature = p.getSignature();
        Object boundReceiver = p.getBoundReceiver();
        if (!SystemPropertiesKt.getUseK1Implementation()) {
            return new LazyKProperty1(name, () -> {
                if (container instanceof KPackageImpl) {
                    KmProperty kmProperty = container.findPropertyMetadata(name, signature);
                    return new KotlinKProperty1(container, signature, boundReceiver, kmProperty, KCallableOverriddenStorage.EMPTY);
                }
                else if (!SystemPropertiesKt.getUseK1ImplementationForMembers() &&
                         container instanceof KClassImpl && !((KClassImpl<?>) container).isComplicatedBuiltinSubclass()) {
                    return findProperty((KClassImpl<?>) container, name, signature, boundReceiver);
                }
                return new DescriptorKProperty1(container, name, signature, boundReceiver);
            });
        }
        return new DescriptorKProperty1(container, name, signature, boundReceiver);
    }

    @Override
    public KMutableProperty1 mutableProperty1(MutablePropertyReference1 p) {
        KDeclarationContainerImpl container = getOwner(p);
        String name = p.getName();
        String signature = p.getSignature();
        Object boundReceiver = p.getBoundReceiver();
        if (!SystemPropertiesKt.getUseK1Implementation()) {
            return new LazyKMutableProperty1(name, () -> {
                if (container instanceof KPackageImpl) {
                    KmProperty kmProperty = container.findPropertyMetadata(name, signature);
                    return new KotlinKMutableProperty1(container, signature, boundReceiver, kmProperty, KCallableOverriddenStorage.EMPTY);
                }
                else if (!SystemPropertiesKt.getUseK1ImplementationForMembers() &&
                         container instanceof KClassImpl && !((KClassImpl<?>) container).isComplicatedBuiltinSubclass()) {
                    return findProperty((KClassImpl<?>) container, name, signature, boundReceiver);
                }
                return new DescriptorKMutableProperty1(container, name, signature, boundReceiver);
            });
        }
        return new DescriptorKMutableProperty1(container, name, signature, boundReceiver);
    }

    @Override
    public KProperty2 property2(PropertyReference2 p) {
        return new DescriptorKProperty2(getOwner(p), p.getName(), p.getSignature());
    }

    @Override
    public KMutableProperty2 mutableProperty2(MutablePropertyReference2 p) {
        return new DescriptorKMutableProperty2(getOwner(p), p.getName(), p.getSignature());
    }

    private static KDeclarationContainerImpl getOwner(CallableReference reference) {
        KDeclarationContainer owner = reference.getOwner();
        return owner instanceof KDeclarationContainerImpl ? ((KDeclarationContainerImpl) owner) : EmptyContainerForLocal.INSTANCE;
    }

    private static boolean isJavaClass(KDeclarationContainerImpl container) {
        return container.getJClass().getAnnotation(Metadata.class) == null &&
               !ConvertFromJavaKt.isMappedBuiltin((KClass<?>) container);
    }

    private static ReflectKProperty<?> findProperty(KClassImpl<?> container, String name, String signature, Object boundReceiver) {
        ReflectKProperty<?> result = (ReflectKProperty<?>) CollectionsKt.firstOrNull(
                container.getData().getValue().getMembersByName(name),
                it -> it instanceof ReflectKProperty<?> && ((ReflectKProperty<?>) it).getSignature().equals(signature)
        );
        if (result == null) {
            throw new KotlinReflectionInternalError(
                    "Property '" + name + "' (JVM signature: " + signature + ") not resolved in " + container
            );
        }
        return (ReflectKProperty<?>) result.rebind(boundReceiver);
    }

    // typeOf

    @Override
    public KType typeOf(KClassifier klass, List<KTypeProjection> arguments, boolean isMarkedNullable) {
        /*
         * We control how this method is called and ensure that `typeOf` is invoked mostly (in scenarios that
         * bother us performance-wise) on the result of `getOrCreateKotlinClass(klass)`, thus we do downcast
         * and extract `java.lang.Class` in a zero-cost manner.
         * If that's our case, we go to caching code-path that caches relatively slow `createType()` call,
         * and, what's more important, all member-based computations on this KType (e.g. `classifier`)
         * are properly cached as well.
         */
        if (klass instanceof ClassBasedDeclarationContainer) {
            return CachesKt.getOrCreateKType(((ClassBasedDeclarationContainer) klass).getJClass(), arguments, isMarkedNullable);
        }
        return KClassifiers.createTypeImpl(klass, arguments, isMarkedNullable, Collections.<Annotation>emptyList(), null, null);
    }

    @Override
    public KTypeParameter typeParameter(Object container, String name, KVariance variance, boolean isReified) {
        if (container instanceof KClass || container instanceof KCallable) {
            return new LazyTypeParameterReference(container, name, variance, isReified);
        }
        throw new IllegalArgumentException("Type parameter container must be a class or a callable: " + container);
    }

    @Override
    public void setUpperBounds(KTypeParameter typeParameter, List<KType> bounds) {
        if (typeParameter instanceof LazyTypeParameterReference) {
            ((LazyTypeParameterReference) typeParameter).setUpperBounds(bounds);
        } else {
            // Do nothing. KTypeParameterImpl implementation will load upper bounds from the metadata.
        }
    }

    // @Override // JPS
    public KType platformType(KType lowerBound, KType upperBound) {
        // TODO: KT-78951 typeOf creates a non-raw type for raw types from Java
        return TypeOfImplKt.createPlatformKType(lowerBound, upperBound, false);
    }

    // @Override // JPS
    public KType mutableCollectionType(KType type) {
        return TypeOfImplKt.createMutableCollectionKType(type);
    }

    // @Override // JPS
    public KType nothingType(KType type) {
        return TypeOfImplKt.createNothingType(type);
    }

    // Misc

    public static void clearCaches() {
        CachesKt.clearCaches();
        ModuleByClassLoaderKt.clearModuleByClassLoaderCache();
        BuiltinsKt.clearBuiltinClassCaches();
    }
}
