# CocoaPods integration

Kotlin/Native provides integration with the [CocoaPods dependency manager](https://cocoapods.org/). You can add
dependencies on Pod libraries stored in the CocoaPods repository or locally as well as use a multiplatform project with 
native targets as a CocoaPods dependency (Kotlin Pod).

You can manage Pod dependencies directly in IntelliJ IDEA and enjoy all the additional features such as code highlighting 
and completion. You can build the whole Kotlin project with Gradle and not ever have to switch to Xcode.
Use Xcode only when you need to write Swift/Objective-C code or run your application on a simulator or device.
 
Depending on your project and purposes, you can add dependencies between:
* [A Kotlin project and a Pod library from the CocoaPods repository](#add-a-dependency-on-a-pod-library-from-the-cocoapods-repository)
* [A Kotlin project and a Pod library stored locally](#add-a-dependency-on-a-pod-library-stored-locally)
* [A Kotlin Pod and an Xcode project with one target](#add-a-dependency-between-a-kotlin-pod-and-xcode-project-with-one-target) 
or [several targets](#add-a-dependency-between-a-kotlin-pod-with-an-xcode-project-with-several-targets)

>You can also add dependencies between a Kotlin Pod and multiple Xcode projects. However, in this case you need to add a 
>dependency by calling `pod install` manually for each Xcode project. In other cases, it's done automatically.
{:.note}

## Install the CocoaPods dependency manager and plugin

1. Install the [CocoaPods dependency manager](https://cocoapods.org/).
    
    <div class="sample" markdown="1" theme="idea" mode="ruby" data-highlight-only>
    
    ```ruby
    $ sudo gem install cocoapods
    ```
    
    </div>

2. Install the [`cocoapods-generate`](https://github.com/square/cocoapods-generate) plugin.

    <div class="sample" markdown="1" theme="idea" mode="ruby" data-highlight-only>
    
    ```ruby
    $ sudo gem install cocoapods-generate
    ```
    
    </div>
    
3. In `build.gradle.kts` (or `build.gradle`) of your IDEA project, apply the CocoaPods plugin as well as the Kotlin
 Multiplatform plugin.

    <div class="sample" markdown="1" theme="idea" data-highlight-only>
    
    ```kotlin
    plugins {
       kotlin("multiplatform") version "{{ site.data.releases.latest.version }}"
       kotlin("native.cocoapods") version "{{ site.data.releases.latest.version }}"
    }
    ```
    
    </div> 

4. Configure `summary`, `homepage`, and `frameworkName`of the `Podspec` file in the `cocoapods` block.  
`version` is a version of the Gradle project.

    <div class="sample" markdown="1" theme="idea" data-highlight-only>
    
    ```kotlin
    plugins {
        kotlin("multiplatform") version "{{ site.data.releases.latest.version }}"
        kotlin("native.cocoapods") version "{{ site.data.releases.latest.version }}"
    }
    
    // CocoaPods requires the podspec to have a version.
    version = "1.0"
    
    kotlin {
        cocoapods {
            // Configure fields required by CocoaPods.
            summary = "Some description for a Kotlin/Native module"
            homepage = "Link to a Kotlin/Native module homepage"
    
            // You can change the name of the produced framework.
            // By default, it is the name of the Gradle project.
            frameworkName = "my_framework"
        }
    }
    ```
    
    </div>
    
5. Re-import the project.

When applied, the CocoaPods plugin does the following:

* Adds both `debug` and `release` frameworks as output binaries for all macOS, iOS, tvOS, and watchOS targets.
* Creates a `podspec` task which generates a [Podspec](https://guides.cocoapods.org/syntax/podspec.html)
file for the project.

The `Podspec` file includes a path to an output framework and script phases that automate building this framework during 
the build process of an Xcode project.

## Add dependencies on Pod libraries

You can add dependencies between a Kotlin project and Pod libraries [stored in the CocoaPods repository](#add-a-dependency-on-a-pod-library-from-the-cocoapods-repository) 
and [stored locally](#add-a-dependency-on-a-pod-library-stored-locally).

[Complete the initial configuration](#install-the-cocoapods-dependency-manager-and-plugin), and when you add a new 
dependency and re-import the project in IntelliJ IDEA; the new dependency will be added automatically. There are no 
additional steps required.

### Add a dependency on a Pod library from the CocoaPods repository

1. Add dependencies on a Pod library that you want to use from the CocoaPods repository with `pod()`  to `build.gradle.kts` 
(`build.gradle`) of your project.  
    > You can also add dependencies on subspecs.
    {:.note}                                                                                                                                                              >

    <div class="sample" markdown="1" theme="idea" data-highlight-only>
    
    ```kotlin
    kotlin {
        ios()
    
        cocoapods {
            summary = "CocoaPods test library"
            homepage = "https://github.com/JetBrains/kotlin"
            pod("AFNetworking", "~> 4.0.0")
            
            pod("SDWebImage/MapKit")
        }
    }
    ```
    
    </div>

2. Re-import the project.

To use these dependencies from the Kotlin code, import the packages `cocoapods.<library-name>`.

<div class="sample" markdown="1" theme="idea" data-highlight-only>

```kotlin
import cocoapods.AFNetworking.*
import cocoapods.SDWebImage.*
```

</div>

You can find a sample project [here](https://github.com/Kotlin/kotlin-with-cocoapods-sample).

### Add a dependency on a Pod library stored locally

1. Add a dependency on a Pod library stored locally with `pod()` to `build.gradle.kts` (`build.gradle`) of your
 project.  
As the third parameter, specify the path to `Podspec` of the local Pod using `project.file(..)`.  
    > You can add local dependencies on subspecs as well.  
    > The `cocoapods` block can include dependencies to Pods stored locally and Pods from the CocoaPods repository at
    > the same time.
    {:.note}

    <div class="sample" markdown="1" theme="idea" data-highlight-only>
    
    ```kotlin
    kotlin {
        ios()
    
        cocoapods {
            summary = "CocoaPods test library"
            homepage = "https://github.com/JetBrains/kotlin"
            pod("pod_dependency", "1.0", project.file("../pod_dependency/pod_dependency.podspec"))
            pod("subspec_dependency/Core", "1.0", project.file("../subspec_dependency/subspec_dependency.podspec"))
            
            pod("AFNetworking", "~> 4.0.0")
            pod("SDWebImage/MapKit")
        }
    }
    ```
    
    </div>

2. Re-import the project.

If you want to use dependencies on local pods from Kotlin code, import the corresponding packages.

<div class="sample" markdown="1" theme="idea" data-highlight-only>

```kotlin
import cocoapods.pod_dependency.*
import cocoapods.subspec_dependency.*
```

</div>

You can find a sample project [here](https://github.com/Kotlin/kotlin-with-cocoapods-sample).
 
## Use a Kotlin Gradle project as a CocoaPods dependency

You can use a Kotlin Multiplatform project with native targets as a CocoaPods dependency (Kotlin Pod). You can include such a dependency
in the Podfile of the Xcode project by its name and path to the project directory containing the generated Podspec.
This dependency will be automatically built (and rebuilt) along with this project.
Such an approach simplifies importing to Xcode by removing a need to write the corresponding Gradle tasks and Xcode build steps manually.

 
You can add dependencies between:
* [A Kotlin Pod and an Xcode project with one target](#add-a-dependency-between-a-kotlin-pod-and-xcode-project-with-one-target)
* [A Kotlin Pod and an Xcode project with several targets](#add-a-dependency-between-a-kotlin-pod-with-an-xcode-project-with-several-targets)

> To correctly import the dependencies into the Kotlin/Native module, the
`Podfile` must contain either [`use_modular_headers!`](https://guides.cocoapods.org/syntax/podfile.html#use_modular_headers_bang)
or [`use_frameworks!`](https://guides.cocoapods.org/syntax/podfile.html#use_frameworks_bang)
directive.
{:.note}

### Add a dependency between a Kotlin Pod and Xcode project with one target

1. Create an Xcode project with a `Podfile` if you haven’t done so yet.
2. Add the path to your Xcode project `Podfile` with `podfile = project.file(..)` to `build.gradle.kts` (`build.gradle`) 
of your Kotlin project.  
    This step helps synchronize your Xcode project with Kotlin Pod dependencies by calling `pod install` for your `Podfile`.
3. Specify the minimum target version for the Pod library.
    > If you don't specify the minimum target version and a dependency Pod requires a higher deployment target, you may get an error.
    {:.note}

    <div class="sample" markdown="1" theme="idea" data-highlight-only>
        
    ```kotlin
    kotlin {
        ios()
    
        cocoapods {
            summary = "CocoaPods test library"
            homepage = "https://github.com/JetBrains/kotlin"
            ios.deploymentTarget = "13.5"
            pod("AFNetworking", "~> 4.0.0")
            podfile = project.file("../ios-app/Podfile")
        }
    }
    ```
    
    </div>

4. Add the name and path of the Kotlin Pod you want to include in the Xcode project to `Podfile`.

    <div class="sample" markdown="1" theme="idea" mode="ruby" data-highlight-only>
    
    ```ruby
    use_frameworks!
    
    platform :ios, '9.0'
    
    target 'ios-app' do
            pod 'kotlin_library', :path => '../kotlin-library'
    end
    ```
    
    </div>

5. Re-import the project.

### Add a dependency between a Kotlin Pod with an Xcode project with several targets

1. Create an Xcode project with a `Podfile` if you haven’t done so yet.
2. Add the path to your Xcode project `Podfile` with `podfile = project.file(..)` to `build.gradle.kts` (`build.gradle`) of
 your Kotlin project.  
    This step helps synchronize your Xcode project with Kotlin Pod dependencies by calling `pod install` for your `Podfile`.
3. Add dependencies to the Pod libraries that you want to use in your project with `pod()`.
4. For each target, specify the minimum target version for the Pod library.

    <div class="sample" markdown="1" theme="idea" data-highlight-only>
    
    ```kotlin
    kotlin {
        ios()
        tvos()
    
        cocoapods {
            summary = "CocoaPods test library"
            homepage = "https://github.com/JetBrains/kotlin"
            ios.deploymentTarget = "13.5"
            tvos.deploymentTarget = "13.4"
       
            pod("AFNetworking", "~> 4.0.0")
            podfile = project.file("../severalTargetsXcodeProject/Podfile") // specify the path to Podfile
        }
    }
    ```
    
    </div>

5. Add the name and path of the Kotlin Pod you want to include in the Xcode project to the `Podfile`.

    <div class="sample" markdown="1" theme="idea" mode="ruby" data-highlight-only>
    
    ```ruby
    target 'iosApp' do
      use_frameworks!
      platform :ios, '13.5'
      # Pods for iosApp
      pod 'kotlin_library', :path => '../kotlin-library'
    end
    
    target 'TVosApp' do
      use_frameworks!
      platform :tvos, '13.4'
      
      # Pods for TVosApp
      pod 'kotlin_library', :path => '../kotlin-library'
    end
    ```
    
    </div>
    
6. Re-import the project.

You can find a sample project [here](https://github.com/Kotlin/multitarget-xcode-with-kotlin-cocoapods-sample).