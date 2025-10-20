#!/bin/bash

export JAVA_HOME=$WORKBENCH/graalvm-jdk-25+37.1/Contents/Home

#exit 0
$JAVA_HOME/bin/java \
-DcacheRedirectorEnabled=true \
-Dfir.bench.dump=false \
-Dfir.bench.jps.dir=$WORKBENCH/modelDump \
-Dfir.bench.passes=50 \
-Dfir.bench.prefix=/ \
-Didea.home.path=$WORKBENCH/kotlin/build/ideaHomeForTests \
-Didea.ignore.disabled.plugins=true \
-Didea.is.unit.test=true \
-Didea.use.native.fs.for.win=false \
-Djava.awt.headless=true \
-Djna.nosys=true \
-Djps.kotlin.home=$WORKBENCH/kotlin/dist/kotlinc \
-Xmx8192m \
-Xms8192m \
--add-opens java.base/java.lang=ALL-UNNAMED \
--add-opens java.base/java.io=ALL-UNNAMED \
--add-opens java.base/java.nio=ALL-UNNAMED \
--add-opens java.base/sun.nio.ch=ALL-UNNAMED \
--add-opens java.desktop/javax.swing=ALL-UNNAMED \
-XX:+UseParallelGC \
-XX:+AlwaysPreTouch \
-XX:+UnlockExperimentalVMOptions -XX:+UseGraalJIT \
-jar $WORKBENCH/kotlin/compiler/fir/modularized-tests/build/libs/owo-2.3.255-SNAPSHOT.jar