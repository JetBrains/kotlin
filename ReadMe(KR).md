[![official project](http://jb.gg/badges/official.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub)
<a href="http://slack.kotlinlang.org/"><img src="http://slack.kotlinlang.org/badge.svg" height="20"></a>
[![TeamCity (simple build status)](https://img.shields.io/teamcity/http/teamcity.jetbrains.com/s/Kotlin_dev_Compiler.svg)](https://teamcity.jetbrains.com/viewType.html?buildTypeId=Kotlin_dev_Compiler&branch_Kotlin_dev=%3Cdefault%3E&tab=buildTypeStatusDiv)
[![Maven Central](https://img.shields.io/maven-central/v/org.jetbrains.kotlin/kotlin-maven-plugin.svg)](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22org.jetbrains.kotlin%22)
[![GitHub license](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat)](http://www.apache.org/licenses/LICENSE-2.0)

# Kotlin 프로그래밍 언어

[Kotlin](https://kotlinlang.org/)에 어서오세요! 몇몇 쓸만한 링크는 다음과 같습니다:

 * [Kotlin 사이트](https://kotlinlang.org/)
 * [입문 가이드](https://kotlinlang.org/docs/tutorials/getting-started.html)
 * [Kotlin 시도하기](https://try.kotlinlang.org/)
 * [Kotlin 표준 라이브러리](https://kotlinlang.org/api/latest/jvm/stdlib/index.html)
 * [이슈 트래커](https://youtrack.jetbrains.com/issues/KT)
 * [포럼](https://discuss.kotlinlang.org/)
 * [Kotlin 블로그](https://blog.jetbrains.com/kotlin/)
 * [Kotlin Twitter 주소](https://twitter.com/kotlin)
 * [공개 Slack 채널](http://slack.kotlinlang.org/)
 * [TeamCity CI 빌드](https://teamcity.jetbrains.com/project.html?tab=projectOverview&projectId=Kotlin)

## Kotlin 수정하기

 * [Kotlin IntelliJ IDEA 플러그인](https://kotlinlang.org/docs/tutorials/getting-started.html)
 * [Kotlin Eclipse 플러그인](https://kotlinlang.org/docs/tutorials/getting-started-eclipse.html)
 * [Kotlin TextMate 번들](https://github.com/vkostyukov/kotlin-sublime-package)

## 개발 환경 요구사항

Kotlin 배포판을 빌드하기 위해서는 다음과 같은 것들이 필요합니다:

- JDK 1.6, 1.7, 1.8 그리고 9
- 환경변수를 다음과 같이 설정합니다:

        JAVA_HOME="JDK 1.8의 경로"
        JDK_16="JDK 1.6의 경로"
        JDK_17="JDK 1.7의 경로"
        JDK_18="JDK 1.8의 경로"
        JDK_9="JDK 9의 경로"

바이트코드를 생성하지 않거나, 표준 라이브러리를 갖고 개발하지 않는 경우(예: 개인 또는 소규모 단위의 개발)에는, JDK 1.8 과 JDK 9 만 설치하고, JDK_16 과 JDK_17 환경변수가 본인이 설치한 JDK 1.8을 가리키도록 하면 됩니다.

[Gradle 속성](https://docs.gradle.org/current/userguide/build_environment.html#sec:gradle_properties_and_system_properties) 을 이용해서 JDK_* 변수들을 설정할 수도 있습니다.

> 참고: MacOS 용 JDK 6 은 Oracle의 사이트에서 구할 수 없습니다. [여기](https://support.apple.com/kb/DL1572) 에서 다운받을 수 있습니다. 

## 빌드하기

이 프로젝트는 Gradle로 빌드되었습니다. Gradle을 실행하여 프로젝트를 빌드하고 Unix/macOS에서 다음과 같은 명령어를 통해 테스트를 합니다:

    ./gradlew <tasks-and-options>
    
Windows에서는 다음과 같습니다:

    gradlew <tasks-and-options>

첫 프로젝트 설정에서 gradle은 다음과 같은 의존성들을 내려받고 설치할 것입니다:


* `intellij-core` 은 커맨드 라인 컴파일러의 일부분이고 가장 핵심적인 API만을 가지고 있습니다.
* `idea-full` 은 플러그인 모듈에서 사용될 완전한 IntelliJ IDEA Community Edition입니다.

These dependencies are quite large, so depending on the quality of your internet connection 
you might face timeouts getting them. In this case you can increase timeout by specifying the following 
command line parameters on the first run: 
    
    ./gradlew -Dhttp.socketTimeout=60000 -Dhttp.connectionTimeout=60000

## Important gradle tasks

- `clean` - clean build results
- `dist` - assembles the compiler distribution into `dist/kotlinc/` folder
- `ideaPlugin` - assembles the Kotlin IDEA plugin distribution into `dist/artifacts/Kotlin` folder
- `install` - build and install all public artifacts into local maven repository
- `runIde` - build IDEA plugin and run IDEA with it
- `coreLibsTest` - build and run stdlib, reflect and kotlin-test tests
- `gradlePluginTest` - build and run gradle plugin tests
- `compilerTest` - build and run all compiler tests
- `ideaPluginTest` - build and run all IDEA plugin tests

**OPTIONAL:** Some artifacts, mainly Maven plugin ones, are built separately with Maven.
Refer to [libraries/ReadMe.md](libraries/ReadMe.md) for details.

## <a name="working-in-idea"></a> Working with the project in IntelliJ IDEA

Working with the Kotlin project requires IntelliJ IDEA 2017.3. You can download IntelliJ IDEA 2017.3 [here](https://www.jetbrains.com/idea/download).

To import the project in Intellij choose project directory in Open project dialog. Then, after project opened, Select 
`File` -> `New...` -> `Module from Existing Sources` in the menu, and select `build.gradle.kts` file in the project's root folder.

In the import dialog, select `use default gradle wrapper`.

To be able to run tests from IntelliJ easily, check `Delegate IDE build/run actions to Gradle` in the Gradle runner settings.

At this time, you can use the latest released 1.2.x version of the Kotlin plugin for working with the code. To make sure you have the latest version installed, use Tools | Kotlin | Configure Kotlin Plugin Updates and press "Check for updates now".

### Compiling and running

From this root project there are Run/Debug Configurations for running IDEA or the Compiler Tests for example; so if you want to try out the latest and greatest IDEA plugin

* VCS -> Git -> Pull
* Run the "IDEA" run configuration in the project
* a child IntelliJ IDEA with the Kotlin plugin will then startup

### Including into composite build

To include kotlin compiler into [composite build](https://docs.gradle.org/current/userguide/composite_builds.html) you need to define `dependencySubstitution` for `kotlin-compiler` module in `settings.gradle`

```
includeBuild('/path/to/kotlin') {
    dependencySubstitution {
        substitute module('org.jetbrains.kotlin:kotlin-compiler') with project(':include:kotlin-compiler')
    }
}
```

# Contributing

We love contributions! There's [lots to do on Kotlin](https://youtrack.jetbrains.com/issues/KT) and on the
[standard library](https://youtrack.jetbrains.com/issues/KT?q=%23Kotlin%20%23Unresolved%20and%20(links:%20KT-2554,%20KT-4089%20or%20%23Libraries)) so why not chat with us
about what you're interested in doing? Please join the #kontributors channel in [our Slack chat](http://slack.kotlinlang.org/)
and let us know about your plans.

If you want to find some issues to start off with, try [this query](https://youtrack.jetbrains.com/issues/KT?q=tag:%20%7BUp%20For%20Grabs%7D%20%23Unresolved) which should find all Kotlin issues that marked as "up-for-grabs".

Currently only committers can assign issues to themselves so just add a comment if you're starting work on it.

A nice gentle way to contribute would be to review the [standard library docs](https://kotlinlang.org/api/latest/jvm/stdlib/index.html)
and find classes or functions which are not documented very well and submit a patch.

In particular it'd be great if all functions included a nice example of how to use it such as for the
[`hashMapOf()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/hash-map-of.html) function.
This is implemented using the [`@sample`](https://github.com/JetBrains/kotlin/blob/1.1.0/libraries/stdlib/src/kotlin/collections/Maps.kt#L91)
macro to include code from a test function. The benefits of this approach are twofold; First, the API's documentation is improved via beneficial examples that help new users and second, the code coverage is increased.

Also the [JavaScript translation](https://github.com/JetBrains/kotlin/blob/master/js/ReadMe.md) could really use your help. See the [JavaScript contribution section](https://github.com/JetBrains/kotlin/blob/master/js/ReadMe.md) for more details.

Some of the code in the standard library is created by generating code from templates. See the [README](libraries/stdlib/ReadMe.md) in the stdlib section for how to run the code generator. The existing templates can be used as examples for creating new ones.

## Submitting patches

The best way to submit a patch is to [fork the project on github](https://help.github.com/articles/fork-a-repo/) then send us a
[pull request](https://help.github.com/articles/creating-a-pull-request/) via [github](https://github.com).

If you create your own fork, it might help to enable rebase by default
when you pull by executing
``` bash
git config --global pull.rebase true
```
This will avoid your local repo having too many merge commits
which will help keep your pull request simple and easy to apply.
