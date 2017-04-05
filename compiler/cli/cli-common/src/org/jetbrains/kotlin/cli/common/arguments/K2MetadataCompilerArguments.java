/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.cli.common.arguments;

public class K2MetadataCompilerArguments extends CommonCompilerArguments {
    public static final long serialVersionUID = 0L;

    @Argument(value = "-d", valueDescription = "<directory|jar>", description = "Destination for generated .kotlin_metadata files")
    public String destination;

    @Argument(
            value = "-classpath",
            shortName = "-cp",
            valueDescription = "<path>",
            description = "Paths where to find library .kotlin_metadata files"
    )
    public String classpath;
}
