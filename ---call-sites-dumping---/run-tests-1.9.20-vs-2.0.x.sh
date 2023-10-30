#!/bin/sh

cd ..
./gradlew :native:native.tests:test \
  --tests "org.jetbrains.kotlin.konan.test.blackbox.FirNativeCodegenBoxTestNoPLGenerated" \
  -Pnative.internal.alternative.home=$HOME/.konan/kotlin-native-prebuilt-macos-aarch64-1.9.20
