# Codegen tests on Android

This module runs codegen box tests (`compiler/testData/codegen/box`) on Android. It does so by compiling all of tests,
except the excluded ones, in one big Android project and running it as an app on an emulator, which is downloaded during
the first run of the tests. See which tests are excluded in `CodegenTestsOnAndroidGenerator`, but mainly those are the
ones annotated with `// IGNORE_BACKEND: ANDROID`, those having Java source files, or using advanced Kotlin/JVM features.

Run the tests via Gradle:

```
./gradlew :compiler:android-tests:test
```

**Make sure your JAVA_HOME points to a JDK 1.8 installation**, otherwise, you'll get an exception, such as
`java.lang.ClassNotFoundException: javax.xml.bind.annotation.XmlSchema`.
