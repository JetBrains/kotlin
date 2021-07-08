#!/bin/bash
set -e
repo="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"

. "$repo/env.sh"


KONAN_UNSAFE=1 KONAN_JNI=1 JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-11.jdk/Contents/Home "$repo/gradlew" -p "$repo" :kotlin-native:distCompiler -Pkotlin.native.enabled=true -Pteamcity=true

