/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen;

import kotlin.text.StringsKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.functions.BuiltInFunctionArity;
import org.jetbrains.kotlin.builtins.functions.FunctionInvokeDescriptor;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.impl.AnonymousFunctionDescriptor;
import org.jetbrains.kotlin.metadata.jvm.deserialization.ModuleMapping;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.types.KotlinType;

import static org.jetbrains.kotlin.codegen.coroutines.CoroutineCodegenUtilKt.SUSPEND_FUNCTION_CREATE_METHOD_NAME;
import static org.jetbrains.kotlin.descriptors.ClassKind.ANNOTATION_CLASS;
import static org.jetbrains.kotlin.descriptors.ClassKind.INTERFACE;

public class JvmCodegenUtil {

    private JvmCodegenUtil() {
    }

    public static boolean isJvmInterface(@Nullable DeclarationDescriptor descriptor) {
        if (descriptor instanceof ClassDescriptor) {
            ClassKind kind = ((ClassDescriptor) descriptor).getKind();
            return kind == INTERFACE || kind == ANNOTATION_CLASS;
        }
        return false;
    }

    public static boolean isJvmInterface(KotlinType type) {
        return isJvmInterface(type.getConstructor().getDeclarationDescriptor());
    }

    @Nullable
    public static ClassDescriptor getDispatchReceiverParameterForConstructorCall(@NotNull ConstructorDescriptor descriptor) {
        //for compilation against binaries
        //TODO: It's best to use this code also for compilation against sources
        // but sometimes structures that have dispatchReceiver (bug?) mapped to static classes
        ReceiverParameterDescriptor dispatchReceiver = descriptor.getDispatchReceiverParameter();
        if (dispatchReceiver != null) {
            ClassDescriptor expectedThisClass = (ClassDescriptor) dispatchReceiver.getContainingDeclaration();
            if (!expectedThisClass.getKind().isSingleton()) {
                return expectedThisClass;
            }
        }

        return null;
    }

    @NotNull
    public static String getModuleName(ModuleDescriptor module) {
        Name stableName = module.getStableName();
        if (stableName == null) {
            // Defensive fallback to possibly unstable name, to not fail with exception
            return prepareModuleName(module.getName());
        } else {
            return prepareModuleName(stableName);
        }
    }

    @NotNull
    public static String prepareModuleName(@NotNull Name name) {
        return StringsKt.removeSurrounding(name.asString(), "<", ">");
    }

    @NotNull
    public static String getMappingFileName(@NotNull String moduleName) {
        return "META-INF/" + moduleName + "." + ModuleMapping.MAPPING_FILE_EXT;
    }

    public static boolean isDeclarationOfBigArityFunctionInvoke(@Nullable DeclarationDescriptor descriptor) {
        return descriptor instanceof FunctionInvokeDescriptor && ((FunctionInvokeDescriptor) descriptor).hasBigArity();
    }

    public static boolean isDeclarationOfBigArityCreateCoroutineMethod(@Nullable DeclarationDescriptor descriptor) {
        return descriptor instanceof SimpleFunctionDescriptor && descriptor.getName().asString().equals(SUSPEND_FUNCTION_CREATE_METHOD_NAME) &&
               ((SimpleFunctionDescriptor) descriptor).getValueParameters().size() >= BuiltInFunctionArity.BIG_ARITY - 1 &&
               descriptor.getContainingDeclaration() instanceof AnonymousFunctionDescriptor && ((AnonymousFunctionDescriptor) descriptor.getContainingDeclaration()).isSuspend();
    }
}
