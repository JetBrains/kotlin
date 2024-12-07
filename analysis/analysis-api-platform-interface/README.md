# Analysis API – Platform Interface

The **platform interface** is an interface which Standalone, IntelliJ, and any additional platforms need to implement for the Analysis API 
to work in their respective environment.

Note: The platform interface has not been stabilized together with the user-facing Analysis API surface. In fact, since both platform
implementations are currently maintained by JetBrains, we will keep the platform interface in an experimental state for a while. Ultimately, 
it is our goal to stabilize the platform interface as well so that third-party developers can support their own environments.


## Definitions

- **Environment** – A setting in which the Analysis API is used, such as an IDE or a CLI environment.
- **Platform** – A set of component implementations required for analysis to be performed in a specific environment.
- **Engine** – An implementation of the Analysis API performing code analysis, relying on the information provided by the platform.
- **Platform Interface** – A specification of abstract components which need to be implemented by a concrete platform, as well as engine 
  services which can be used by the platform implementation. The platform interface is used by the engine to retrieve declarations and 
  project structure, react to code modifications, etc., to perform code analysis in accordance with the idiosyncrasies of the environment.
- **User** – A person or software which analyzes source code using the API available via the `analysis-api` module. A user typically invokes 
  the [`analyze`](../analysis-api/src/org/jetbrains/kotlin/analysis/api/analyze.kt) entry point to work with symbols, types, calls, and 
  other concepts. We explicitly differentiate users and platforms, especially in the context of *user-facing* vs. *platform* APIs. The
  platform interface is normally hidden from users, as they should not need to access nor be aware of the inner workings of the Analysis 
  API.  


## Platform

From the view of the Analysis API, a platform has the following responsibilities:

- Provide declarations, packages, direct inheritors, and other information about the code from an appropriate source, such as an index.
- Provide the project structure in the form of [`KaModule`s](../analysis-api/src/org/jetbrains/kotlin/analysis/api/projectStructure/KaModule.kt) 
  and related concepts.
- Monitor modifications to project files and binary roots, and publish modification events so that the Analysis API engine can invalidate 
  its caches.
- Define symbol lifetime with a specific implementation for [`KaLifetimeToken`s](../analysis-api/src/org/jetbrains/kotlin/analysis/api/lifetime/KaLifetimeToken.kt). 
  Even if modifications are not possible, the platform must provide a notion of “static” lifetime.

A specific platform may have additional features, such as providing a DSL for project structure creation to users like Standalone, but this 
is up to the platform.

Additional responsibilities may be added in the future as the Analysis API (and especially LL FIR) evolves. Platforms need to be evolved 
with the rest of the Analysis API.

### Combining platform implementation and usage

Some platforms like the IntelliJ Kotlin plugin are both an Analysis API user and a platform implementation. This is perfectly legal, but 
care should be taken to properly separate modules which use the Analysis API and modules which additionally provide implementations for 
platform components.

### Information about the code

Platforms need to provide information about project and library content to the Analysis API engine, because the Analysis API itself has no
direct way of knowing the content of all source files, and neither does it include an out-of-the-box indexing solution. 

For example, if we're analyzing a class, and it references another class `org.example.Foo`, the Analysis API will need to look up the 
declaration of `org.example.Foo`. It is the responsibility of the platform to implement a 
[`KotlinDeclarationProvider`](src/org/jetbrains/kotlin/analysis/api/platform/declarations/KotlinDeclarationProvider.kt) which returns a PSI
declaration for that class, in any way which works for the platform.

### Modification and Lifetime

Platforms are responsible for modification and lifetime because they control the location and content of their projects. It is not an 
Analysis API user's responsibility to define lifetime when they have no control over the content from inside an 
[`analyze`](../analysis-api/src/org/jetbrains/kotlin/analysis/api/analyze.kt) block.

Of course, when a platform implementation is also a user, the lines get blurred again. However, even if, for example, an IDE intention 
modifies the code, the code modification itself should still be published as a modification event via the platform interface, and hence the
IDE acts in its capacity as a platform. Inside an `analyze` block where content should be read-only, the IDE is purely an Analysis API user 
and exerts no control over modification.

### Current Platforms

The IntelliJ Kotlin plugin and the Standalone Analysis API are the two currently existing platforms, both maintained by JetBrains. There are 
no plans to support additional platforms, but third-party developers are welcome to incorporate the Analysis API into their own 
environments.


## Platform Interface

The platform interface is defined in this module, `analysis-api-platform-interface`. It consists of two parts:

- **Platform components:** Interfaces which need to be implemented by the concrete platform, as they are required by the Analysis API 
  implementation to perform analysis.
- **Engine services**: Functionality provided by an Analysis API engine to support platform implementations. These services do not need to
  be implemented by the platform, but can rather be used as needed.

Platform components and engine services may occur in the same packages, but they can be distinguished by their prefixes: Platform components
use the `Kotlin` prefix (e.g. [`KotlinDeclarationProvider`](src/org/jetbrains/kotlin/analysis/api/platform/declarations/KotlinDeclarationProvider.kt)),
while engine services use the `Ka` prefix (e.g. [`KaLifetimeTracker`](src/org/jetbrains/kotlin/analysis/api/platform/lifetime/KaLifetimeTracker.kt)).

To ease discovery, platform components extend the [`KotlinPlatformComponent`](src/org/jetbrains/kotlin/analysis/api/platform/KotlinPlatformComponent.kt) 
marker interface, while engine services extend [`KaEngineService`](src/org/jetbrains/kotlin/analysis/api/platform/KaEngineService.kt). This 
makes it easy to identify which components needs to be implemented, i.e. all `KotlinPlatformComponent`s (with an exception for optional 
platform components, marked with `KotlinOptionalPlatformComponent`).

In addition to these abstract interfaces, the platform interface also contains additional abstract concepts which aren't strictly platform 
components (e.g. `KotlinDeclarationProvider`), as well as base implementations for a few platform components. All these declarations are 
usually placed in the same package as the related platform component.

### Structure

The platform interface has the following important subpackages:

- `declarations`: Platform components which provide declarations or information about declarations, see "information about the code" above.
- `lifetime`: Services for lifetime handling. Notably, there are already two standard implementations for lifetime tokens:
  [`KotlinAlwaysAccessibleLifetimeToken`](src/org/jetbrains/kotlin/analysis/api/platform/lifetime/KotlinAlwaysAccessibleLifetimeToken.kt) 
  for static code (used by Standalone) and [`KotlinReadActionConfinementLifetimeToken`](src/org/jetbrains/kotlin/analysis/api/platform/lifetime/KotlinReadActionConfinementLifetimeToken.kt) 
  for modifiable code (used by IntelliJ).
- `modification`: Message topics and services for publishing modification events. See 
  [`KotlinModificationTopics`](src/org/jetbrains/kotlin/analysis/api/platform/modification/KotlinModificationTopics.kt).
- `packages`: Platform components which provide information about packages, see "information about the code" above.
- `permissions`: Services for permission handling. [`KaAnalysisPermissionChecker`](src/org/jetbrains/kotlin/analysis/api/platform/permissions/KaAnalysisPermissionChecker.kt)
  can be used by lifetime tokens to implement `isAccessible`, but is also used internally to check permissions when 
  [`analyze`](../analysis-api/src/org/jetbrains/kotlin/analysis/api/analyze.kt) is called.
- `projectStructure`: Platform components which provide information about the project's structure, such as 
  [`KaModule`s](../analysis-api/src/org/jetbrains/kotlin/analysis/api/projectStructure/KaModule.kt) which are the Analysis API's conception 
  of modules. The core component to implement is [`KotlinProjectStructureProvider`](src/org/jetbrains/kotlin/analysis/api/platform/projectStructure/KotlinProjectStructureProvider.kt), 
  but there are various other components which cover other important aspects of project information. 

### Service registration

In general, it is the responsibility of the platform to initialize the Analysis API. Even Standalone does the bulk of the initialization
work itself, albeit triggered by the Analysis API user. 

Initialization mostly takes the form of registration of internal and public services. The Analysis API has its own service registration 
mechanism via plugin XMLs. These XML files especially register internal engine services, which would be cumbersome for a platform to 
enumerate. Standalone currently implements its own handler for applying these plugin XMLs, and no such support is currently available via 
the platform interface out of the box. 

You can track the following issue: [KT-69385](https://youtrack.jetbrains.com/issue/KT-69385). Please leave a comment if it affects you.
