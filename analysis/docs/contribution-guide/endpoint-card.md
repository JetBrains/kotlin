# Analysis API Endpoint Card

## Purpose

The endpoint card is a structured analysis template for evaluating existing Analysis API endpoints. It helps contributors systematically
assess API endpoints from multiple perspectives: clarity, usage patterns, design quality, and potential improvements. Use this card when
analyzing an endpoint for refactoring, enhancement, or deprecation to ensure thorough evaluation of its current state and identify areas for
improvement.

## Endpoint Traits

### Description

Provide a clear, concise description of what the endpoint does in 1–2 sentences. Focus on the primary functionality and expected behavior.
If describing the endpoint's purpose is challenging, or it requires multiple sentences, this may indicate the method has too many
responsibilities and could benefit from decomposition.

**Example:**

> Checks if a symbol is visible in the given context.

### Understandability

Evaluate how easily developers can understand the endpoint's purpose and behavior without diving into implementation details.

* **Documentation**: Is there a comprehensive KDoc that explains the purpose, parameters, return values, and any important behavior or edge
  cases?
* **Naming**: Do the function name, parameter names, and type annotations clearly convey the intended meaning and usage?
* **Clarity**: Can a developer understand what the endpoint does just by reading its signature and documentation?

**Examples:**

> For `javaGetterName`, there is no KDoc. The extension always returns some name, even if the property doesn’t have a getter. It’s unclear
> how the method works for `expect` declarations or for non-JVM platforms. It’s unclear whether it checks the `@JvmName` annotation or
> handles bytecode mangling for inline classes.

> The function name `analyseImports()` is too generic. It does not suggest that the method is part of the import optimizer implementation
> and is not designed for general use.

### Usages

#### Where is the endpoint used?

Identify where and how frequently the endpoint is used across different contexts.
The scale of usage is more important than exact counts and helps prioritize design work.

**Usage contexts to check:**

- [ ] Kotlin IntelliJ IDEA plugin
- [ ] External plugins (use the *Find External Usages* action)
- [ ] KSP
- [ ] Dokka
- [ ] Swift Export
- [ ] Other clients

**Questions to consider:**

* How many usages are there? (approximate count is enough)
* Is the method suitable for both the IDE and Standalone scenarios?
* Are there any usage spikes in particular modules or contexts?

#### Are there common patterns?

Look for repetitive code patterns that appear consistently across different usages of the endpoint.
Common patterns may indicate missing convenience methods, unclear API design, or opportunities for improvement.

**Questions to consider:**

* Do developers frequently write similar boilerplate code before or after calling this endpoint?
* Are there common type checks, null checks, or transformations that appear repeatedly?
* Do these patterns suggest the endpoint's design could be improved?

**Example:**

> Before all calls of `isPublicApi`, there is a `is KtSymbolWithVisibility` type check that looks verbose.

#### Are usages correct?

Analyze whether the endpoint is being used as intended and whether there are better alternatives available.

**Questions to evaluate:**

* Do the actual usages align with the endpoint's intended semantics and purpose?
* Are there more appropriate or efficient alternatives that could solve the same problems?
* If better alternatives exist, why aren't they being used?
    * Are the alternatives not well-documented or discoverable?
    * Do corner cases exist that the alternatives don't handle?
    * Is there missing functionality that forces developers to use suboptimal approaches?

**Example:**

> `resolveToCall()` is often used to get the symbol for a successful call. Argument mapping and other facilities that `resolveToCall()`
> provides are ignored. Possible reasons:
>
> 1. It is not straightforward that `resolveToSymbol()` can only be called on a reference (`mainReference`). People use what is available in
     > completion.
> 2. `resolveToCall()` and `resolveToSymbol()` return different symbols in certain cases.

### Design

#### Is it easy to abuse the endpoint?

Assess the potential for misuse and the robustness of the endpoint's design.

**Questions to consider:**

* How easy is it to misuse this endpoint? Are there obvious ways it could be called incorrectly?
* What are the consequences of misuse? (e.g., performance issues, incorrect results, crashes)
* Are there enough safeguards in place? (e.g., parameter validation, clear error messages)
* Are there any dangerous edge cases that aren't well-documented?

#### Is the endpoint generic enough?

Evaluate whether the endpoint strikes the right balance between generality and specificity.

**Core vs. Convenience:**

* Is this part of core Analysis API functionality, or is it a convenience method?
* If it's a convenience method:
    * Can it be implemented outside the Analysis API with equivalent performance?
    * Does it provide significant value beyond what users could easily implement themselves?
* If it's specific to a particular functionality (e.g., auto-completion, refactoring):
    * Is this specialization clear from the endpoint's name and location?
    * Should it be moved to a different class or module?

**Generality and Extensibility:**

* Are there opportunities to extract more generic functionality that could benefit other use cases?
    * Can the endpoint be extended to provide additional related functionality without breaking existing usage?
* Is the abstraction level appropriate for its intended use cases?

**Example:**

> `containingJvmClassName` returns the facade name for declarations in multi-file facades. While it might be helpful in understanding
> how the method can be called from Java, it does not answer the question of where the actual bytecode will be.

## Template

Use this template when creating documents or issues for endpoint analysis:

```markdown
# Analysis API Endpoint Card: `EndpointName`

## Description

📝 Provide a clear, concise description of what the endpoint does in 1-2 sentences.

## Understandability

📝 Evaluate how easily developers can understand the endpoint's purpose and behavior.

**Related Issues found:**

📝 Provide a list of related issues.

## Usages

### Where is the endpoint used?

📝 Give approximate usage count and distribution across the library clients:

- [ ] Kotlin IntelliJ IDEA plugin
- [ ] External plugins (use the *Find External Usages* action)
- [ ] KSP
- [ ] Dokka
- [ ] Swift Export
- [ ] Other clients

### Are there common patterns?

📝 Describe any repetitive code patterns around endpoint usage.

### Are usages correct?

📝 Analyze whether the endpoint is being used as intended.

## Design

### Is it easy to abuse the endpoint?

📝 Assess potential for misuse and robustness.

### Is the endpoint generic enough?

📝 Evaluate the balance between generality and specificity.

## Recommendations

📝 Summarize key findings and recommended actions:

1.
2.
3.

## Severity

📝 Assess the endpoint severity based on its usage and impact:

- [ ] High (frequent usage, significant impact)
- [ ] Medium (moderate usage or impact)
- [ ] Low (infrequent usage, minimal impact)
```