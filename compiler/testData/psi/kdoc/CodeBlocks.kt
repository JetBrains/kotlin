/**
 * 1.
 *    ```
 *    line 1
 *    line 2
 *    ```
 *
 * 2.
 *    ~~~
 *    line 1
 *    line 2
 *    ~~~
 *
 * 3.
 *    ```
 *
 *
 *
 *
 *    ```
 */

/**
 * ```
 *    line 1
 *    line 2
 *  line 3
 * line 4
 * ```
 * ~~~
 *    line 1
 *    line 2
 *  line 3
 * line 4
 * ~~~
 */

/**
 *   ```
 *   line
 *   ```
 *
 *   ```
 *  line
 *   ```
 *
 *  ```
 *   line
 *  ```
 *
 *   ~~~
 *   line
 *   ~~~
 *
 *   ~~~
 *  line
 *   ~~~
 *
 *  ~~~
 *   line
 *  ~~~
 */

/**
 * a ``` b ``` c
 *
 * a ```
 * b
 * ``` c
 *
 * ```
 * a
 * ``` c
 *
 *  ```
 *  a
 *  b ```
 */

/**
 *    Indented code block
 * ```
 *  Fenced code block
 * ```
 *
 *    Another indented code block
 *
 *   ```
 *      Not well formed code block
 *   ```
 */


/**
 * ```
 * A code line with incorrect ending fence ```
 * ```
 *
 * Not a code line
 */

/**
 * ~~~
 * A code line with different fences
 * ```
 *
 * ~~~
 */