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

package org.jetbrains.jet.cli.common;

import com.sampullara.cli.Args;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.cli.jvm.K2JVMCompilerArguments;

import java.io.PrintStream;

/**
 * @author Pavel Talanov
 */
public final class ArgsUtil {

    private ArgsUtil() {
    }

    public static void printUsage(@NotNull PrintStream target, @NotNull CompilerArguments exampleInstance) {
        // We should say something like
        //   Args.usage(target, K2JVMCompilerArguments.class);
        // but currently cli-parser we are using does not support that
        // a corresponding patch has been sent to the authors
        // For now, we are using this:
        PrintStream oldErr = System.err;
        System.setErr(target);
        try {
            // TODO: use proper argv0
            Args.usage(exampleInstance);
        }
        finally {
            System.setErr(oldErr);
        }
    }
}
