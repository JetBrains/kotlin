The incremental compilation code is in a not-ideal state, where
some implementation details are in the :kotlin-build-common module,
and some stable APIs are in the :compiler:incremental-compilation-impl.

Also, incremental-compilation-impl depends on the build-common.

Where possible, it's useful to start separating "common-implementation" code
from "common-api" code.

Be careful with TrackerImpl classes, as some of them are used by the JPS and KSP.
