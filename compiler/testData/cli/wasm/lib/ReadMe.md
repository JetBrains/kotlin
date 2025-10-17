# wasmlib

LibraryExample.klib is used as input to CLI tests of 2nd compilation stage.
When needed, recompile it with
```bash
dist/kotlinc/bin/kotlinc-js -Xwasm compiler/testData/cli/wasm/lib/LibraryExample.kt -ir-output-dir compiler/testData/cli/wasm/lib/LibraryExample.klib -ir-output-name LibraryExample -libraries libraries/stdlib/build/libs/kotlin-stdlib-wasm-js-*.klib
```
