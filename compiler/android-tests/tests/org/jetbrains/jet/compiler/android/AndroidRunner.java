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

package org.jetbrains.jet.compiler.android;

import com.google.common.io.Files;
import com.intellij.openapi.util.io.FileUtil;
import junit.framework.TestSuite;
import org.jetbrains.jet.compiler.CodegenTestsOnAndroidRunner;
import org.jetbrains.jet.compiler.PathManager;

import java.io.File;

/**
 * @author Natalia.Ukhorskaya
 */

public class AndroidRunner extends TestSuite {

    public static TestSuite suite() throws Throwable {
        File tmpFolder = Files.createTempDir();
        System.out.println("Created temporary folder for running android tests: " + tmpFolder.getAbsolutePath());
        File rootFolder = new File("");
        PathManager pathManager = new PathManager(rootFolder.getAbsolutePath(), tmpFolder.getAbsolutePath());

        FileUtil.copyDir(new File(pathManager.getAndroidModuleRoot()), new File(pathManager.getTmpFolder()));
        
        try {
            CodegenTestsOnAndroidGenerator.generate(pathManager);
        } 
        catch(Throwable e) {
            FileUtil.delete(new File(pathManager.getTmpFolder()));
            throw new RuntimeException(e);
        }
        
        System.out.println("Run tests on android...");
        return CodegenTestsOnAndroidRunner.getTestSuite(pathManager);
    }
    
}
