#!/bin/bash

export JAVA_HOME=/Users/Simon.Ogorodnik/Downloads/graalvm-jdk-25+37.1/Contents/Home

$JAVA_HOME/bin/java \
-DcacheRedirectorEnabled=true \
-Dfir.bench.dump=true \
-Dfir.bench.jps.dir=/Users/Simon.Ogorodnik/TestWorkspace/mt-kotlin2/test-project-model-dump \
-Dfir.bench.passes=1 \
-Dfir.bench.prefix=/Users/Simon.Ogorodnik/TestWorkspace/mt-kotlin2 \
-Dfir.modularized.jvm.args=-XX:-TieredCompilation -XX:+AlwaysPreTouch \
-Didea.home.path=/Users/Simon.Ogorodnik/Workspace/KotlinRepo/work2/build/ideaHomeForTests \
-Didea.ignore.disabled.plugins=true \
-Didea.is.unit.test=true \
-Didea.use.native.fs.for.win=false \
-Djava.awt.headless=true \
-Djna.nosys=true \
-Djps.kotlin.home=/Users/Simon.Ogorodnik/Workspace/KotlinRepo/work2/dist/kotlinc \
-Dkotlin.test.update.test.data=false \
-Dorg.jetbrains.kotlin.skip.muted.tests=false \
-XX:+HeapDumpOnOutOfMemoryError \
-XX:+UseCodeCacheFlushing \
-XX:ReservedCodeCacheSize=512m \
-XX:MaxMetaspaceSize=512m \
-XX:CICompilerCount=2 \
-XX:-TieredCompilation \
-XX:+AlwaysPreTouch \
-Xms8192m \
-Xmx8192m \
-Dfile.encoding=UTF-8 \
-Duser.country=US \
-Duser.language=en \
-Duser.variant \
-ea \
--add-opens java.base/java.lang=ALL-UNNAMED \
--add-opens java.base/java.io=ALL-UNNAMED \
--add-opens java.base/java.nio=ALL-UNNAMED \
--add-opens java.base/sun.nio.ch=ALL-UNNAMED \
--add-opens java.desktop/javax.swing=ALL-UNNAMED \
-agentlib:native-image-agent=config-output-dir=./cfg -jar /Users/Simon.Ogorodnik/Workspace/KotlinRepo/work2/compiler/fir/modularized-tests/build/libs/owo-2.3.255-SNAPSHOT.jar