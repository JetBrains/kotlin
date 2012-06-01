/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

import jet.JetObject;
import jet.runtime.typeinfo.*;
import org.jetbrains.jet.rt.annotation.AssertInvisibleInResolver;

/**
 * @author Stepan Koltsov
 */
public class JvmStdlibNames {

    public static final JvmClassName JET_VALUE_PARAMETER = new JvmClassName(JetValueParameter.class);

    public static final String JET_VALUE_PARAMETER_NAME_FIELD = "name";
    public static final String JET_VALUE_PARAMETER_HAS_DEFAULT_VALUE_FIELD = "hasDefaultValue";
    public static final String JET_VALUE_PARAMETER_NULLABLE_FIELD = "nullable";
    public static final String JET_VALUE_PARAMETER_TYPE_FIELD = "type";
    public static final String JET_VALUE_PARAMETER_RECEIVER_FIELD = "receiver";


    public static final JvmClassName JET_TYPE_PARAMETER = new JvmClassName(JetTypeParameter.class);

    public static final String JET_TYPE_PARAMETER_NAME_FIELD = "name";


    public static final JvmClassName JET_METHOD = new JvmClassName(JetMethod.class);

    public static final String JET_METHOD_KIND_FIELD = "kind";
    public static final String JET_METHOD_NULLABLE_RETURN_TYPE_FIELD = "nullableReturnType";
    public static final String JET_METHOD_RETURN_TYPE_FIELD = "returnType";
    public static final String JET_METHOD_TYPE_PARAMETERS_FIELD = "typeParameters";
    public static final String JET_METHOD_PROPERTY_TYPE_FIELD = "propertyType";
    
    public static final int JET_METHOD_KIND_REGULAR = 0;
    public static final int JET_METHOD_KIND_PROPERTY = 1;
    public static final int JET_METHOD_KIND_DEFAULT = JET_METHOD_KIND_REGULAR;

    public static final JvmClassName JET_CONSTRUCTOR = new JvmClassName(JetConstructor.class);

    /**
     * @deprecated
     * @see JetConstructor#hidden()
     */
    public static final String JET_CONSTRUCTOR_HIDDEN_FIELD = "hidden";
    

    public static final JvmClassName JET_CLASS = new JvmClassName(JetClass.class);
    
    public static final String JET_CLASS_SIGNATURE = "signature";
    

    public static final JvmClassName JET_OBJECT = new JvmClassName(JetObject.class);


    public static final JvmClassName ASSERT_INVISIBLE_IN_RESOLVER = new JvmClassName(AssertInvisibleInResolver.class);

    public static final JvmClassName KOTLIN_SIGNATURE = new JvmClassName(KotlinSignature.class);
    public static final String KOTLIN_SIGNATURE_VALUE_METHOD = "value";

    private JvmStdlibNames() {
    }
}
