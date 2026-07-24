## Supported versions

We apply security updates to the latest language (2.x.0) or tooling release (2.x.20) of the compiler and build plugins.

We also ship fixes with the next incremental (2.x.y) or bug fix release (2.x.yz).

We apply all fixes to the master branch so they are included in upcoming releases.

### Standard library security support

Starting with Kotlin 2.4.0, the Kotlin standard library for the JVM has an 18-month support window for each release line. Language releases
(2.x.0) and the following tooling releases (2.x.20) belong to the same release line (2.x).

If we discover a security vulnerability that affects the Kotlin standard library for the JVM, we ship the following simultaneously:

* A bug fix release based on the latest Kotlin release in the release line that includes the security fix.
* Bug fix releases for every release line within its active support window.

For example, if we discover a security vulnerability and the latest Kotlin release is Kotlin 2.4.20, we release a bug fix version for Kotlin 2.4.20
only. We don't release a bug fix version for Kotlin 2.4.0.

For the latest information, see [Standard library security support](https://kotlinlang.org/docs/releases.html#standard-library-security-support).

## Reporting a vulnerability

You can find instructions for reporting a vulnerability on the [Security page](https://kotlinlang.org/docs/security.html).
