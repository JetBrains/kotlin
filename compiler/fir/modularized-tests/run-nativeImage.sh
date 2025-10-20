#!/bin/bash


export JAVA_HOME=$WORKBENCH/graalvm-jdk-25+37.1/Contents/Home

$WORKBENCH/kotlin/owo-no-pgo \
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
-Djava.awt.headless=true \
-Xmx8192m \
-Xms8192m 