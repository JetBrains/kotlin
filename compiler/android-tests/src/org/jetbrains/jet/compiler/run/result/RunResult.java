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

package org.jetbrains.jet.compiler.run.result;

/**
 * @author Natalia.Ukhorskaya
 */

public class RunResult {
    private final boolean status;
    private final String output;

    public RunResult(boolean ok, String output) {
        status = ok;
        this.output = output;
    }

    //true - ok
    //false - fail
    public boolean getStatus() {
        return status;
    }

    public String getOutput() {
        return output;
    }
}
