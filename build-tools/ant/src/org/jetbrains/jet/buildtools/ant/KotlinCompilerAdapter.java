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

package org.jetbrains.jet.buildtools.ant;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.Javac;
import org.apache.tools.ant.taskdefs.compilers.DefaultCompilerAdapter;
import org.apache.tools.ant.taskdefs.compilers.Javac13;
import org.apache.tools.ant.types.Path;

public class KotlinCompilerAdapter extends DefaultCompilerAdapter {
    private Path externalAnnotations;

    public void setExternalAnnotations(Path externalAnnotations) {
        this.externalAnnotations = externalAnnotations;
    }

    public Path createExternalAnnotations() {
        if (externalAnnotations == null) {
            externalAnnotations = new Path(getProject());
        }
        return externalAnnotations.createPath();
    }

    @Override
    public boolean execute() throws BuildException {
        Javac javac = getJavac();

        Kotlin2JvmTask kotlinTask = new Kotlin2JvmTask();
        kotlinTask.setOutput(javac.getDestdir());
        kotlinTask.setClasspath(javac.getClasspath());
        kotlinTask.setSrc(javac.getSrcdir());
        kotlinTask.setExternalAnnotations(externalAnnotations);

        kotlinTask.execute();

        javac.log("Running javac...");

        Javac13 javac13 = new Javac13();
        javac13.setJavac(javac);
        return javac13.execute();
    }
}
