#!/bin/bash

# Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
# Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
#

mkdir -p ~/.gradle
echo "kotlin.build.scan.url=https://ge.jetbrains.com/" >> ~/.gradle/gradle.properties
echo "kotlin.build.cache.url=https://ge.jetbrains.com/cache/" >> ~/.gradle/gradle.properties

./gradlew classes testClasses -Dscan.uploadInBackground=false
