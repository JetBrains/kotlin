# Kotlin Analysis API Development Guidelines

This series of guides provides comprehensive instructions for developing and maintaining the Kotlin Analysis API. They are intended for:

- **JetBrains developers** working on the Analysis API core, implementations, and platform integrations
- **External contributors** submitting patches and improvements to the Analysis API
- **Tool developers** who want to understand the design philosophy and best practices behind the API

The guidelines cover API design principles, implementation patterns, naming conventions, and evolution strategies that help maintain
consistency and quality across the codebase.

## What is the Analysis API?

The Kotlin Analysis API is a library for analyzing Kotlin code at the semantic level. It provides a structured way to query information
about Kotlin code, including symbols, types, and semantic relationships between them. Built on top of Kotlin's
[PSI](https://plugins.jetbrains.com/docs/intellij/psi.html) syntax trees, the API offers a clean abstraction over the compiler's internal
representations, making it accessible both for IDE plugin makers and command-line tool developers.

Check the [Analysis API documentation](https://kotl.in/analysis-api) for more information.

## Guidelines

### **[API Development Guide](api-development.md)**

This document covers principles and practices for developing new Analysis API endpoints, including design patterns, naming conventions,
documentation standards, and requirements for API implementations.

### **[API Evolution Guide](api-evolution.md)**

This document covers the lifetime of individual Analysis API endpoints from introduction to deprecation.

### **[Endpoint Card](endpoint-card.md)**

A template for systematic analysis of existing API endpoints to support refactoring, enhancement, or deprecation decisions.