# The `.kar` publication prototype

## Motivation
The Kotlin team has long recognized opportunities to simplify Kotlin Multiplatform publications. The current publication format
introduces considerable complexity and requires remote repositories to store many files and large amounts of data. This also has
negative consequences for downstream consumers.

Earlier, more ambitious plans to simplify and unify the publication layout, known as "uber KLIBs," were put on hold because changing
the publication format was not a sufficiently high priority to justify the required engineering effort.

However, recent Maven Central policy changes have significantly increased the priority of several aspects of those plans:

- Reducing the number of files required for a library publication
- Reducing the amount of data required for a library publication
- Reducing the number of artifact IDs occupied by Kotlin Multiplatform libraries

With these goals in mind, the previous designs were distilled into the proposed Kotlin Archive (`.kar`) publication format.

## The `.kar` file format
### Archive structure
A `.kar` file has a well-defined layout, allowing each contained artifact to be located at a predictable path.

```text
<library>.kar                         # Uncompressed ZIP stream after XZ decompression
├── metadata/
│   ├── project-structure-metadata.json
│   ├── <fragment-name>/
│   │   └── <metadata-artifact>.klib
│   └── <native-source-set>-cinterop/
│       └── ... commonized C interop metadata
├── platform/
│   ├── native/
│   │   └── <konan-target>/           # For example, linux_x64 or macos_arm64
│   │       └── ... unpacked platform KLIB contents
│   ├── js/
│   │   └── ... unpacked JS KLIB contents
│   └── wasm/
│       ├── js/
│       │   └── ... unpacked Wasm-JS KLIB contents
│       └── wasi/
│           └── ... unpacked Wasm-WASI KLIB contents
└── cinterops/
    └── platform/
        └── native/
            └── <konan-target>/
                └── <cinterop-output-name>/
                    └── ... C interop KLIB contents
```

#### Platform KLIBs
Platform KLIBs produced by compiling a target to IR are stored under
`/platform/<platform-type>/<platform-specific-classifier>/`. The platform-specific classifier is omitted when it is not needed.

Examples:
- The platform KLIB for the `linuxX64` target is stored in `/platform/native/linux_x64/`.
- The platform KLIB for the `wasmJs` target is stored in `/platform/wasm/js/`.
- The platform KLIB for the `wasmWasi` target is stored in `/platform/wasm/wasi/`.

#### Platform C interop KLIBs
Kotlin/Native targets can provide any number of C interop KLIBs alongside their compilation output. These KLIBs are stored under
`/cinterops/`, following the same platform hierarchy as platform KLIBs.

Examples:
- The `curl` C interop for the `linuxX64` target is stored in `/cinterops/platform/native/linux_x64/curl/`.

The C interop name is arbitrary. To resolve C interops for a target, list all entries under
`/cinterops/platform/native/<konan-target>/`.

#### Metadata KLIBs
Like the existing all-metadata JAR (also known as `CompositeMetadataArtifact`), a `.kar` file contains the output of all metadata
compilations. Unlike the previous publication format, this proposal does not consider any source set host-specific by default.

Metadata compilation outputs are stored under `/metadata/<fragment-name>/`.

Examples:
- The metadata KLIB for the `commonMain` fragment is stored in `/metadata/commonMain/`.
- The metadata KLIB for the `nativeMain` fragment is stored in `/metadata/nativeMain/`.

#### Commonized C interop metadata KLIBs
Like `CompositeMetadataArtifact`, a `.kar` file also contains all commonized C interop metadata when C interop commonization is enabled.
All commonized C interop libraries for a fragment are stored under `/metadata/<fragment-name>-cinterop/`.

Example:
- The commonized `curl` C interop metadata KLIB associated with the `commonMain` fragment is stored in
  `/metadata/commonMain-cinterop/curl/`. The C interop name is arbitrary; resolution discovers all KLIBs in the fragment's C interop
  directory.

#### Future work: Multiplatform resources
This prototype does not yet include Multiplatform resources in the `.kar` artifact. Including them would be a natural extension,
analogous to storing resources in JAR files, and they are likely to compress efficiently alongside the other archive contents.

### `.kar` file compression (`.kar.xz`)
Like JAR and AAR files, a `.kar` file uses the ZIP container format. Multiplatform publications, however, differ from typical Java JAR
files: artifacts produced for different targets often contain similar data. For example, a `linuxX64` KLIB can be very similar to its
`macosX64` counterpart.

To exploit this similarity, the ZIP entries remain uncompressed and the complete ZIP stream is then compressed with XZ. The resulting
artifact is published with the `.kar.xz` extension.

As an additional optimization, ZIP entries should be ordered by content similarity rather than by a depth-first traversal of the
directory tree. Placing similar files next to one another can improve XZ compression.

## Gradle Publication
Because the `.kar` file can contain all target artifacts, separate artifacts and coordinates are no longer necessary. Instead, the
proposal publishes only the root artifact. In principle, this allows an entire Kotlin Multiplatform publication to use a single
`groupId` and `artifactId` pair.

```terminaloutput
└── org
    └── jetbrains
        └── kotlin
            ├── sample
            │   ├── 1.0.0
            │   │   ├── sample-1.0.0-kotlin-tooling-metadata.json
            │   │   ├── sample-1.0.0-sources.jar
            │   │   ├── sample-1.0.0.kar.xz
            │   │   ├── sample-1.0.0.module
            │   │   └── sample-1.0.0.pom
            │   └── maven-metadata.xml
```

All targets continue to expose API and runtime variants. However, instead of using `available-at` pointers to target-specific
coordinates, these variants point to the `.kar.xz` file as their artifact.

### JVM compatibility
For compatibility, the proposal allows selected targets to remain in separate components. Publishing the JVM target as a separate
component, for example, may be a reasonable default for existing libraries because Maven consumers may expect the `-jvm` coordinate.

```terminaloutput
└── org
    └── jetbrains
        └── kotlin
            ├── sample
            │   ├── 1.0.0
            │   │   ├── sample-1.0.0-kotlin-tooling-metadata.json
            │   │   ├── sample-1.0.0-sources.jar
            │   │   ├── sample-1.0.0.kar.xz
            │   │   ├── sample-1.0.0.module
            │   │   └── sample-1.0.0.pom
            │   └── maven-metadata.xml
            └── sample-jvm
                ├── 1.0.0
                │   ├── sample-jvm-1.0.0-sources.jar
                │   ├── sample-jvm-1.0.0.jar
                │   ├── sample-jvm-1.0.0.module
                │   └── sample-jvm-1.0.0.pom
                └── maven-metadata.xml
```

In this case, the root publication continues to reference the JVM variant through an `available-at` pointer.

Further simplification remains possible. For example, all artifacts could still be published in the root component while the
compatibility `-jvm` publication declares a single dependency on the root coordinates. Such changes can be introduced incrementally.

## Gradle Resolution
Gradle artifact transforms resolve the relevant parts of a `.kar.xz` file.

### Resolving platform KLIBs
```text
.kar.xz --XZDecompressAction--> .kar --KarToPlatformKlibTransformation--> .klib
```

The `.kar.xz` file is first decompressed. The relevant KLIB is then extracted from either the packed `.kar` file or an unpacked `.kar`
directory.

### Resolving metadata KLIBs
`GranularMetadataTransformation` is the custom state machine that resolves Kotlin Multiplatform libraries. The proposed `.kar`
artifact is similar to `CompositeMetadataArtifact`, but it stores project structure metadata and metadata KLIBs at different paths.
The transformation should therefore support both layouts, resolving the relevant content from `.kar` or `.jar` files as appropriate.
