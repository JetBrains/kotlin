#
# Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
# Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
#

#!/bin/zsh
set +e
export JAVA_HOME=$JDK_17
PROJECT_ROOT=$(git rev-parse --show-toplevel)
java -jar /Users/Simon.Ogorodnik/Workspace/kotlin-compiler-devkit/cli-runner/build/libs/cli-runner-all.jar --project-root "$PROJECT_ROOT" --gradle-args "--init-script $PROJECT_ROOT/gradle/init-scripts/junit-xml-reports.gradle" --test-data-path $1

echo "Test result reports found:"
find . -type d -name "test-results" -exec find {} -type f -name "*.xml" \;
