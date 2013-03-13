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

package org.jetbrains.jet.lang.resolve.java;

import jet.runtime.typeinfo.JetConstructor;

public class JvmStdlibNames {

    public static final JvmClassName JET_VALUE_PARAMETER = JvmClassName.byFqNameWithoutInnerClasses("jet.runtime.typeinfo.JetValueParameter");

    public static final String JET_VALUE_PARAMETER_NAME_FIELD = "name";
    public static final String JET_VALUE_PARAMETER_HAS_DEFAULT_VALUE_FIELD = "hasDefaultValue";
    public static final String JET_VALUE_PARAMETER_TYPE_FIELD = "type";
    public static final String JET_VALUE_PARAMETER_RECEIVER_FIELD = "receiver";
    public static final String JET_VALUE_PARAMETER_VARARG = "vararg";


    public static final JvmClassName JET_TYPE_PARAMETER = JvmClassName.byFqNameWithoutInnerClasses("jet.runtime.typeinfo.JetTypeParameter");


    public static final JvmClassName JET_METHOD = JvmClassName.byFqNameWithoutInnerClasses("jet.runtime.typeinfo.JetMethod");

    public static final String JET_FLAGS_FIELD = "flags";

    public static final String JET_METHOD_RETURN_TYPE_FIELD = "returnType";
    public static final String JET_METHOD_TYPE_PARAMETERS_FIELD = "typeParameters";
    public static final String JET_METHOD_PROPERTY_TYPE_FIELD = "propertyType";

    public static final int FLAGS_DEFAULT_VALUE = 0;

    public static final int FLAG_PROPERTY_BIT = 1;
    public static final int FLAG_FORCE_OPEN_BIT = 1 << 1;
    public static final int FLAG_FORCE_FINAL_BIT = 1 << 2;
    public static final int FLAG_PRIVATE_BIT =  1 << 3;
    public static final int FLAG_INTERNAL_BIT = 1 << 4;
    public static final int FLAG_PROTECTED_BIT = 1 << 5;

    // for method, three bits (one reserved)
    public static final int FLAG_METHOD_KIND_MASK = 7 << 6;
    public static final int FLAG_METHOD_KIND_DECLARATION =   0 << 6;
    public static final int FLAG_METHOD_KIND_FAKE_OVERRIDE = 1 << 6;
    public static final int FLAG_METHOD_KIND_DELEGATION =    2 << 6;
    public static final int FLAG_METHOD_KIND_SYNTHESIZED =   3 << 6;

    public static final int FLAG_CLASS_KIND_MASK = 7 << 6;
    public static final int FLAG_CLASS_KIND_DEFAULT = 0 << 6;
    public static final int FLAG_CLASS_KIND_OBJECT = 1 << 6;

    public static final JvmClassName JET_CONSTRUCTOR = JvmClassName.byFqNameWithoutInnerClasses("jet.runtime.typeinfo.JetConstructor");

    /**
     * @deprecated
     * @see JetConstructor#hidden()
     */
    public static final String JET_CONSTRUCTOR_HIDDEN_FIELD = "hidden";


    public static final JvmClassName JET_CLASS = JvmClassName.byFqNameWithoutInnerClasses("jet.runtime.typeinfo.JetClass");

    public static final JvmClassName JET_CLASS_OBJECT = JvmClassName.byFqNameWithoutInnerClasses("jet.runtime.typeinfo.JetClassObject");

    public static final JvmClassName JET_PACKAGE_CLASS = JvmClassName.byFqNameWithoutInnerClasses("jet.runtime.typeinfo.JetPackageClass");

    public static final String ABI_VERSION_NAME = "abiVersion";

    public static final String JET_CLASS_SIGNATURE = "signature";


    public static final JvmClassName JET_OBJECT = JvmClassName.byFqNameWithoutInnerClasses("jet.JetObject");


    public static final JvmClassName ASSERT_INVISIBLE_IN_RESOLVER = JvmClassName.byFqNameWithoutInnerClasses("org.jetbrains.jet.rt.annotation.AssertInvisibleInResolver");

    public static final JvmClassName KOTLIN_SIGNATURE = JvmClassName.byFqNameWithoutInnerClasses("jet.runtime.typeinfo.KotlinSignature");
    public static final String KOTLIN_SIGNATURE_VALUE_METHOD = "value";

    private JvmStdlibNames() {
    }
}
