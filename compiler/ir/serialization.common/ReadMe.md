(De)serialization of IR into protobuf 2 format, its linkage and validations. It is used for:

- Storing compiled library code, that can later be linked into the final application.
  - For JVM backend, in the experimental `-Xserialize-ir` mode, IR is stored in .class files inside of special annotation. Its primary purpose is so that one module can copy the IR of inline functions compiled to another module.
  In the default mode, when no IR is stored, the inlining is done by copying the JVM bytecode instead.
  - For other backends, the libraries are compiled into .klib files, which consist of IR + metadata.

If you modify a .proto file, you will need to regenerate classes with generators/protobuf/GenerateProtoBuf.kt