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

import org.jetbrains.jet.lang.DefaultModuleConfiguration;
import org.jetbrains.jet.lang.resolve.ImportPath;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class JavaBridgeConfiguration {

    public static final List<ImportPath> DEFAULT_JAVA_IMPORTS = Collections.unmodifiableList(Arrays.asList(new ImportPath("java.lang.*")));
    public static final List<ImportPath> ALL_JAVA_IMPORTS;

    static {
        List<ImportPath> allJavaImports = new ArrayList<ImportPath>();
        allJavaImports.addAll(DEFAULT_JAVA_IMPORTS);
        allJavaImports.addAll(DefaultModuleConfiguration.DEFAULT_JET_IMPORTS);
        ALL_JAVA_IMPORTS = Collections.unmodifiableList(allJavaImports);
    }
}
