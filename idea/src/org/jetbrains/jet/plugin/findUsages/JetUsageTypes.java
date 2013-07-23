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

package org.jetbrains.jet.plugin.findUsages;

import com.intellij.usages.impl.rules.UsageType;
import org.jetbrains.jet.plugin.JetBundle;

public class JetUsageTypes {
    private JetUsageTypes() {
    }

    // Type-related usages
    public static final UsageType ANNOTATION_TYPE = new UsageType(JetBundle.message("usageType.annotation.type"));
    public static final UsageType TYPE_CONSTRAINT = new UsageType(JetBundle.message("usageType.type.constraint"));
    public static final UsageType TYPE_ARGUMENT = new UsageType(JetBundle.message("usageType.type.argument"));
    public static final UsageType VALUE_PARAMETER_TYPE = new UsageType(JetBundle.message("usageType.value.parameter.type"));
    public static final UsageType NON_LOCAL_PROPERTY_TYPE = new UsageType(JetBundle.message("usageType.nonLocal.property.type"));
    public static final UsageType LOCAL_VARIABLE_TYPE = new UsageType(JetBundle.message("usageType.local.variable.type"));
    public static final UsageType FUNCTION_RETURN_TYPE = new UsageType(JetBundle.message("usageType.function.return.type"));
    public static final UsageType SUPER_TYPE = new UsageType(JetBundle.message("usageType.superType"));
    public static final UsageType TYPE_DEFINITION = new UsageType(JetBundle.message("usageType.type.definition"));
    public static final UsageType AS = new UsageType(JetBundle.message("usageType.as"));
    public static final UsageType IS = new UsageType(JetBundle.message("usageType.is"));
    public static final UsageType CLASS_OBJECT_ACCESS = new UsageType(JetBundle.message("usageType.class.object"));
    public static final UsageType EXTENSION_RECEIVER_TYPE = new UsageType(JetBundle.message("usageType.extension.receiver.type"));
    public static final UsageType SUPER_TYPE_QUALIFIER = new UsageType(JetBundle.message("usageType.super.type.qualifier"));

    // Function-related usages
    public static final UsageType INSTANTIATION = new UsageType(JetBundle.message("usageType.instantiation"));
    public static final UsageType FUNCTION_CALL = new UsageType(JetBundle.message("usageType.function.call"));

    // Value-related usages
    public static final UsageType RECEIVER = new UsageType(JetBundle.message("usageType.receiver"));
    public static final UsageType SELECTOR = new UsageType(JetBundle.message("usageType.selector"));

    // Miscellaneous usages
    public static final UsageType IMPORT_DIRECTIVE = new UsageType(JetBundle.message("usageType.import"));
    public static final UsageType CALLABLE_REFERENCE = new UsageType(JetBundle.message("usageType.callable.reference"));
}
