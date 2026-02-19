/**
 * Some text
 *
 *    [NotALink]
 *    Code Block
 *      Indented code block with two extra spaces
 *
 * Next text
 *
 * ```
 * Code block surrouned by fences
 * ```
 *     Also a code block next after code block with fences
 */

/**
 * Some text
 *     [X] (Recognize the link because there is only a single line break and it's not a code block, KT-79783)
 *       [Y] Next Link
 *
 *     But it's a code block
 */

/**    Code block at the beginning!
 *
 * Some text
 *
 ***    Code Block (not well-formed)
 **
 *    Code Block 2 (not well-formed)
 * Next text
 */

// ISSUE: KT-79783

/**
 * - Test [Number]
 *     - Test [String]
 */
class X

/**
 * The instructions are coming from three different places:
 * - **A**:
 *     - Source: [org.company.A], parameterized instructions
 *     - Completed with variables from the [C] by the [D] when invoking `.doIt()`
 */
