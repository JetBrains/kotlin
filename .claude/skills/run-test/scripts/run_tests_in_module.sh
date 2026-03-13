#
# Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
# Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
#

#!/bin/zsh
set +e
#if [ -n "$JDK_17" ]; then
#  export JAVA_HOME=$JDK_17
#else
#  export JAVA_HOME=$(find ~/.gradle/jdks -maxdepth 2 -type d -name "jdk-17*" 2>/dev/null | head -1)/Contents/Home
#fi
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
CLI_RUNNER_DIR="$SCRIPT_DIR/../cli-runner"
TEST_LOG_JAR="$CLI_RUNNER_DIR/test-log/build/libs/test-log.jar"
if [ ! -f "$TEST_LOG_JAR" ]; then
  echo "Building test-log..."
  (cd "$CLI_RUNNER_DIR" && ./gradlew :test-log:jar -q)
fi
MODULE=$1
shift
PROJECT_ROOT=$(git rev-parse --show-toplevel)
"$PROJECT_ROOT"/gradlew --init-script "$PROJECT_ROOT"/gradle/init-scripts/junit-xml-reports.gradle cleanTest "$MODULE":test "$@" --continue

echo "Test result reports found:"
find . -type d -name "test-results" -exec find {} -type f -name "*.xml" \;