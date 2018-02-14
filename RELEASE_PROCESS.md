## Making release of Kotlin/Native ##

### Move version up in the repository ###

   * Increment `konanVersion` in topmost `gradle.properties` and in `KonanVersion.kt`
   * Create entry for new release in CHANGELOG.md file (consult git history for features included)
   * Update RELEASE_NOTES.md with actual information on the released bits

### Create tagged release branch ###

    git checkout -b v0.X-fixes
    git tag -a v0.X -m "version 0.X"
    git push --set-upstream origin v0.X-fixes --tags

### Build for all supported platforms ###

Repeat those steps for macOS, Linux x86-64 and Windows x64 machines/VMs:

    git pull
    git checkout -b v0.X-fixes origin/v0.X-fixes
    ./gradlew clean bundle

Make sure all samples are buildable from the bundle with both Gradle and shell script builds.

### Create GitHub release ###

 Create release at [`GitHub release page`](https://github.com/JetBrains/kotlin-native/releases).
We usually mark 0.X releases as pre-releases.

 Upload builds created on the previous step to the GitHub release page.


### Upload builds ###

 Upload build to CDN at `upload.cds.intellij.net/builds/releases/<version>/<macos|linux|windows>`:
 
    CDN_URL=upload.cds.intellij.net CDN_USER=... CDN_PASS=... ./gradlew :uploadBundle
 
Bundles are available at `http://download.jetbrains.com/kotlin/native/releases/<version>/<platform>/<build>`
in few minutes after upload.

 Upload Gradle plugin to BinTray

    BINTRAY_USER=... BINTRAY_KEY=... ./gradlew :gradlePluginUpload

### Blog post ###
 
 Notify #kotlin-native channel on public Slack.
 
 Write a meaningfully sized blog post describing new release at `https://blog.jetbrains.com`.
Login at `https://blog.jetbrains.com/kotlin/wp-login.php`. Synchronize with @abreslav and @yole.
Publish and enjoy!
 
