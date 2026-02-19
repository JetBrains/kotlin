This module contains common lowering phases and utils shared across all kotlin backends (JVM, Native, JS, WASM).

Not all lowering phases are used on all backends, and not necessarily in the same order.
Instead, this module serves as a library, where each backend can choose to include any of the shared phases, along custom ones.

However, on klib-backends there is also a short "common prefix" of phases that are invoked in all of them, before IR is serialized into klib.