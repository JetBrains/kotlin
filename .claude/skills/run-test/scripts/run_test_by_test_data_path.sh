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
CLI_RUNNER_JAR="$CLI_RUNNER_DIR/build/libs/cli-runner-all.jar"
TEST_LOG_JAR="$CLI_RUNNER_DIR/test-log/build/libs/test-log.jar"
if [ ! -f "$CLI_RUNNER_JAR" ] || [ ! -f "$TEST_LOG_JAR" ]; then
  echo "Building cli-runner and test-log..."
  (cd "$CLI_RUNNER_DIR" && ./gradlew shadowJar :test-log:jar -q)
fi
PROJECT_ROOT=$(git rev-parse --show-toplevel)
java -jar "$CLI_RUNNER_JAR" --project-root "$PROJECT_ROOT" --gradle-args "--init-script $PROJECT_ROOT/gradle/init-scripts/junit-xml-reports.gradle" --test-data-path $1

echo "Test result reports found:"
find . -type d -name "test-results" -exec find {} -type f -name "*.xml" \;
