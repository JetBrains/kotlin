# Joint Kotlin Build

How to work with Kotlin joint build:

1. Switch to `native-support/master` branch:
   - `git checkout native-support/master`
2. Download Kotlin/Native repository and place it into `kotlin-native` subdirectory:
   - `git clone https://github.com/JetBrains/kotlin-native.git`
   - `git checkout native-support/master`
3. Similarly for Ultimate repository - place it into `kotlin-ultimate`:
   - `git clone ssh://git@jetbrains.team/kotlin-ultimate.git`
   - `git checkout native-support/master`
4. Refresh dependencies for Kotlin/Native:
   - `./gradlew :kotlin-native:dependencies:update`
5. Build Kotlin/Native (this step is not included into joint build, therefore you need to run it manually):
   - `cd ./kotlin-native/`
   - `./gradlew dist distPlatformLibs`
6. Build Kotlin/Native plugin with all necessary dependencies and run it in IDEA Community Edition:
   - `./gradlew runIde`