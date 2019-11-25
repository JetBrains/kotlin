/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.tooling.model.kotlin.dsl;

import org.gradle.api.Incubating;


/**
 * Parameters for Kotlin DSL models.
 *
 * @since 6.0
 */
@Incubating
public final class KotlinDslModelsParameters {

    public static final String PREPARATION_TASK_NAME = "prepareKotlinBuildScriptModel";

    public static final String CORRELATION_ID_GRADLE_PROPERTY_NAME = "org.gradle.kotlin.dsl.provider.cid";

    public static final String PROVIDER_MODE_SYSTEM_PROPERTY_NAME = "org.gradle.kotlin.dsl.provider.mode";

    public static final String CLASSPATH_MODE_SYSTEM_PROPERTY_VALUE = "classpath";

    public static final String STRICT_CLASSPATH_MODE_SYSTEM_PROPERTY_VALUE = "strict-classpath";

    public static final String CLASSPATH_MODE_SYSTEM_PROPERTY_DECLARATION = "-D" + PROVIDER_MODE_SYSTEM_PROPERTY_NAME + "=" + CLASSPATH_MODE_SYSTEM_PROPERTY_VALUE;

    public static final String STRICT_CLASSPATH_MODE_SYSTEM_PROPERTY_DECLARATION = "-D" + PROVIDER_MODE_SYSTEM_PROPERTY_NAME + "=" + STRICT_CLASSPATH_MODE_SYSTEM_PROPERTY_VALUE;
}
