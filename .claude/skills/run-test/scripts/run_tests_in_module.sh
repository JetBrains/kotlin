#
# Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
# Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
#

#!/bin/zsh
set +e
export JAVA_HOME=$JDK_17
MODULE=$1
shift
/Users/Simon.Ogorodnik/Workspace/KotlinRepo/work3/gradlew --init-script /Users/Simon.Ogorodnik/Workspace/KotlinRepo/work3/gradle/init-scripts/junit-xml-reports.gradle cleanTest "$MODULE":test "$@" --continue

echo "Test result reports found:"
find . -type d -name "test-results" -exec find {} -type f -name "*.xml" \;