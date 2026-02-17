#
# Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
# Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
#

#!/bin/zsh
set +e
export JAVA_HOME=$JDK_17
java -jar /Users/Simon.Ogorodnik/Workspace/kotlin-compiler-devkit/cli-runner/build/libs/cli-runner-all.jar --project-root /Users/Simon.Ogorodnik/Workspace/KotlinRepo/work3 --gradle-args "--init-script /Users/Simon.Ogorodnik/Workspace/KotlinRepo/work3/gradle/init-scripts/junit-xml-reports.gradle" --test-data-path $1

echo "Test result reports found:"
find . -type d -name "test-results" -exec find {} -type f -name "*.xml" \;