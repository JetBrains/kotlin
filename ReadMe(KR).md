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

첫 프로젝트 설정에서 gradle은 다음과 같은 의존성들(의존하는 패키지 등)을 내려받고 설치할 것입니다:


* `intellij-core` 은 커맨드 라인 컴파일러의 일부분이고 가장 핵심적인 API만을 가지고 있습니다.
* `idea-full` 은 플러그인 모듈에서 사용될 완전한 IntelliJ IDEA Community Edition입니다.

이 의존성들은 꽤 크기 때문에, 인터넷 연결 상태에 따라서는 
이들을 가져오다가 시간 초과가 발생할 수 있습니다. 이 경우에는 다음과 같은 명령어 인자(parameter)를 첫 실행에 추가함으로써 해결할 수 있습니다: 
    
    ./gradlew -Dhttp.socketTimeout=60000 -Dhttp.connectionTimeout=60000

## 중요한 gradle 태스크들

- `clean` - 빌드 결과를 지웁니다.
- `dist` - 배포 컴파일러들을 `dist/kotlinc/` 폴더 안에 모읍니다.
- `ideaPlugin` - Kotlin IDEA 배포 플러그인들을 `dist/artifacts/Kotlin` 폴더 안에 모읍니다.
- `install` - 모든 공유(public) 아티팩트들을 maven 로컬 저장소 안에 빌드하고 설치합니다.
- `runIde` - IDEA 플러그인을 빌드하고, IDEA를 플러그인과 함께 실행합니다.
- `coreLibsTest` - 표준 라이브러리(stdlib)를 빌드하고 실행하며, 테스트들(또는 유닛 테스트)을 reflect, kotlin-test합니다.
- `gradlePluginTest` - gradle 플러그인 테스트들을 빌드하고 실행합니다.
- `compilerTest` - 모든 컴파일러 테스트들을 빌드하고 실행합니다.
- `ideaPluginTest` - 모든 IDEA 플러그인 테스트들을 빌드하고 실행합니다.

**참고:** Maven 플러그인들을 포함한 몇몇 아티팩트들은 Maven과 별개로 빌드되었습니다.
자세한 사항은 [libraries/ReadMe.md](libraries/ReadMe.md)를 참조하시기 바랍니다.

## <a name="working-in-idea"></a> IntelliJ IDEA 환경에서 Kotlin 프로젝트 이용하기

Kotlin 프로젝트를 이용하려면 IntelliJ IDEA 2017.3이 필요합니다. IntelliJ IDEA 2017.3은 [여기](https://www.jetbrains.com/idea/download)에서 다운받을 수 있습니다.

Intellij에서 프로젝트를 import하기 위해 Open project 다이얼로그에서 프로젝트 디렉토리를 선택합니다. 프로젝트가 열린 뒤, 메뉴에서 
`File` -> `New...` -> `Module from Existing Sources`를 선택하고, 프로젝트의 최상위(root) 폴더에 있는 `build.gradle.kts`파일을 선택합니다.

import 다이얼로그에서, `use default gradle wrapper`를 선택합니다.

IntelliJ를 통해 테스트를 쉽게 실행하기 위해서, Gradle runner 설정에서 `Delegate IDE build/run actions to Gradle`를 체크합니다.

이제, Kotlin 플러그인의 최신 배포 1.2.x 버전을 이용할 수 있습니다. 최신 버전을 설치했는지 확인하려면, Tools | Kotlin | Configure Kotlin Plugin Updates 에서 "Check for updates now"를 눌러서 확인할 수 있습니다.

### 컴파일과 실행

이 최상위(root) 프로젝트에는 IDEA 실행 또는 컴파일러 테스트를 위한 Run/Debug 설정 예제가 있습니다; 만약 최신 IDEA 플러그인을 시도해보고 싶다면

* VCS -> Git -> Pull
* 프로젝트의 "IDEA" 실행 설정(run configuration)을 실행합니다.
* 또 다른 IntelliJ IDEA가 Kotlin 플러그인과 함께 시작됩니다.

### 복합(Composite) 빌드에 포함하기

Kotlin 컴파일러를 [composite build](https://docs.gradle.org/current/userguide/composite_builds.html)에 포함하기 위해서, `settings.gradle`에서 `kotlin-compiler` 모듈을 위해 `dependencySubstitution`을 정의해야 합니다.

```
includeBuild('/path/to/kotlin') {
    dependencySubstitution {
        substitute module('org.jetbrains.kotlin:kotlin-compiler') with project(':include:kotlin-compiler')
    }
}
```

# 참여하기

우리는 참여를 적극 환영합니다! [Kotlin에 해야 할 많은 일들](https://youtrack.jetbrains.com/issues/KT)이 있고,
[표준 라이브러리](https://youtrack.jetbrains.com/issues/KT?q=%23Kotlin%20%23Unresolved%20and%20(links:%20KT-2554,%20KT-4089%20or%20%23Libraries))에도 할 일이 많습니다.
그러니 혹시 하고 싶은 일이 있다면 우리와 채팅하시는 게 어떻습니까? 
[Slack 채팅](http://slack.kotlinlang.org/)의 #kontributors 채널에 들어와서 당신의 계획에 대해 알려주세요.

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
