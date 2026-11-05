# Guiding principles

1. [Build tools should seamlessly support creating software](#1-build-tools-should-seamlessly-support-creating-software)
2. [Consistency beats marginal improvements](#2-consistency-beats-marginal-improvements)
3. [Simple should be easy, complex should be possible](#3-simple-should-be-easy-complex-should-be-possible)
4. [We care about all audiences, but with different levels of direct support](#4-we-care-about-all-audiences-but-with-different-levels-of-direct-support)

## Details and examples

### 1. Build tools should seamlessly support creating software

The role of build tools is enablement, not becoming a domain of expertise on their own.
They should empower developers to focus on building great software with Kotlin, not on mastering the tools themselves.

To state the obvious, the build speed is also a key part of the experience and should not hinder the software
development process. Our goal is to improve the developer experience. Build speed is only one aspect of this, but not
the only aspect that is important. At any given time we should prioritize the aspects that will improve the overall
developer experience the most.

### 2. Consistency beats marginal improvements

If there is an established way to do things, re-inventing the wheel for a minor improvement will do more harm than good.
Breaking out of the mold makes sense only, and only if it would bring significant value to the user, and always requires
careful consideration.

We are opinionated when it benefits the users. There is a value in good out-of-the-box defaults and there is value in
guidance (especially when faced with multiple options). For most developers build setup is a required scaffolding for
the core part of their job and the less cognitive load it creates the better.

**Example:** There is no _the_ way to structure a KMP project in Gradle. We provide a strong recommendation in wizards,
but acknowledge that there is no one right way to structure larger applications and we should support most of typical
approaches without favoring any of them.

### 3. Simple should be easy, complex should be possible

Kotlin is a concise multi-platform language, designed for simplicity and safety. The developers should enjoy the same
simplicity in the project setup as much as possible. This applies to both the most frequent setups as well as the
getting started experience.

Kotlin is used in countless use cases, however, the more complex the use case the less frequent it is and there is no
single simple solution that will fit all of them. We aim to support the majority of the use cases either directly or
indirectly by providing building blocks from which a solution can be assembled.

We aim for simple and intuitive solutions, however, if making a complex use case easier would mean that we are making a
simple use case harder, we should favor simplicity in the simple one.

### 4. We care about all audiences, but with different levels of direct support

We care about all Kotlin users, but it's not always possible to deliver top experience for every audience. Build tools
must align closely with the department's strategic priorities, providing first-class support for core focus areas, and
possibility of creating decent experience in other areas with the help of the community.

**Examples:**

- KMP experience in Maven is not a core focus area, so it does not have a good support at all.
- Server-side experience in both Gradle and Maven is a core focus, so we aim for feature parity in both building
  systems.
