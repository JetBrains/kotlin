# Compiler's high-level structure graph

The Diagram below can be viewed with a Markdown editor with a Mermaid renderer,
e.g. GitHub; IntelliJ + "Markdown" plugin + "Mermaid Visualizer" plugin; VS Code; etc.; or just https://mermaid.ai/live

Please improve it using Mermaid syntax: 
- https://mermaid.js.org/syntax/flowchart.html
- https://github.com/mermaid-js/mermaid/blob/develop/README.md

```mermaid
flowchart TD

    subgraph 1st compilation stage
        KotlinSources@{ shape: docs, label: "KotlinSources"}
        JavaSources@{ shape: docs, label: "JavaSources"}
        KLibDependencies@{ shape: lin-cyl, label: "KLib\ndependencies" }
        JARDependencies@{ shape: lin-cyl, label: "JAR\ndependencies" }
        MetadataDeserialization@{ shape: procs, label: "Metadata Deserializers"}
        PreSerialization[Pre-serialization IR lowerings]
        FIRPlugins@{ shape: subproc, label: "FIR Plugins"}
        IRPlugins@{ shape: subproc, label: "IR Plugins"}
        FIR2IR[FIR2IR + Actualizer]
        JVMIRLowerings[IR lowerings: common and JVM-specific]
        JavaSources --> FIRFrontend

        KotlinSources --> FIRFrontend
        KLibDependencies --> MetadataDeserialization
        JARDependencies --> MetadataDeserialization
        MetadataDeserialization --> FIRFrontend
        
        FIRFrontend <--> FIRPlugins
        FIRFrontend --FIR--> FIR2IR
        FIR2IR --IR--> IRPlugins
        IRPlugins --IR--> CompilationSwitch{Is this JVM compilation}
        CompilationSwitch --Yes --> JVMIRLowerings
        JVMIRLowerings --IR --> JVMBackend
        CompilationSwitch --No --> PreSerialization
        PreSerialization --IR --> KlibSerialization
    end

    subgraph C/ObjC Import
        CSRC@{ shape: docs, label: "C/ObjC Sources"}
        DEF@{ shape: doc, label: ".def file"}
        CInterop@{ label: "CInterop 🔗" }
        style CInterop fill:#e1f5fe,stroke:#01579b

        CSRC --> CInterop
        DEF --> CInterop
        click CInterop "https://github.com/JetBrains/kotlin/blob/master/docs/native/compilation-model.md" "Click to see Native compilation model"
    end
    KLIB@{ shape: lin-cyl, label: "KLib" }
    KlibSerialization --> KLIB
    KLIB --> KLibDeserialization
    CInterop --> KLIB

    JAR@{ shape: lin-cyl, label: "JAR" }
    JVMBackend --> JAR

    subgraph 2nd compilation stage
        IRLowerings[IR lowerings: common and backend-specific]
        Native_Backend@{ label: "Native Backend 🔗" }
        JS_Backend[JS Backend]
        WASM_Backend[WASM Backend]

        KLibDeserialization --IR--> IR_Linking
        IR_Linking --IR--> IRLowerings
        IRLowerings --IR--> Native_Backend
        IRLowerings --IR--> JS_Backend
        IRLowerings --IR--> WASM_Backend
    end
    subgraph Executables
        CPU@{ shape: lin-cyl, label: "Binary:\nexe, dylib, so" }
        STATIC@{ shape: lin-cyl, label: "Klib's static cache"}
        JS@{ shape: lin-cyl, label: "JS bundle"}
        WASM_JS@{ shape: lin-cyl, label: "WASM_JS"}
        WASM_WASI@{ shape: lin-cyl, label: "WASM_WASI"}

        click Native_Backend "https://github.com/JetBrains/kotlin/blob/master/docs/native/compilation-model.md" "Click to see Native compilation model"
        style Native_Backend fill:#e1f5fe,stroke:#01579b
        Native_Backend --> CPU
        Native_Backend --> STATIC
        JS_Backend --> JS
        WASM_Backend --> WASM_JS
        WASM_Backend --> WASM_WASI
    end
```

### Diagram Explanation:
1. **1st Compilation Stage (sources+dependencies -> Jar or KLib)**:
    - Begins with input source code and JAR/Klib dependencies
    - Applies various stages of the compilation pipeline within Kotlin, leading to a `.jar` or `.klib` as output for the module.

2. **2nd Compilation Stage (KLibs -> executable artifact or library cache) for [Native](../native/compilation-model.md), JS, WASM backends**:
    - Deserializes/links KLibs
    - Performs numerous IR lowerings.
    - Converts IR to backend-specific representation.
    - Generates an executable artifact.
