#!/bin/bash


export JAVA_HOME=$WORKBENCH/graalvm-jdk-25+37.1/Contents/Home

$JAVA_HOME/bin/native-image \
--no-fallback \
--add-opens java.base/java.lang=ALL-UNNAMED \
--add-opens java.base/java.io=ALL-UNNAMED \
--add-opens java.base/java.nio=ALL-UNNAMED \
--add-opens java.base/sun.nio.ch=ALL-UNNAMED \
--add-opens java.desktop/javax.swing=ALL-UNNAMED \
-H:+AddAllCharsets \
-H:+AllowJRTFileSystem \
--gc=serial \
-Djava.library.path=$JAVA_HOME/lib \
-Didea.ignore.disabled.plugins=true \
-Didea.is.unit.test=true \
-Didea.use.native.fs.for.win=false \
-Djava.awt.headless=true \
-jar compiler/fir/modularized-tests/build/libs/owo-2.3.255-SNAPSHOT.jar
-o owo-no-pgo
