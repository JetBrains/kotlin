#!/bin/bash
~/Downloads/graalvm-jdk-21.0.5+9.1/Contents/Home/bin/native-image \
--add-opens=java.base/java.io=ALL-UNNAMED --add-opens=java.base/java.lang=ALL-UNNAMED -H:+AddAllCharsets \
-jar owo-2.1.255-SNAPSHOT.jar
