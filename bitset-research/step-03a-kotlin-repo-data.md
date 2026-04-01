# Шаг 3a. Каталог использования BitSet в репозитории Kotlin

## Резюме

Каталогизированы все 26 записей в репозитории kotlin, содержащих BitSet-связанный код (25 файлов по `grep -rl "BitSet"` с покрытием `*.kt`, `*.java`, `*.api` + 1 файл, использующий BitSet-extension без упоминания "BitSet" по имени). Использования распределены по 8 разделам: compiler utility (1 файл), compiler PSI/parser (1), compiler JVM backend (3), compiler Common IR backend (1), compiler K/N backend (6, из них 2 с `java.util.BitSet` и 4 с `CustomBitSet`), analysis/symbol light classes (5), stdlib regex engine (2), native commonizer (1). Отдельно каталогизированы 4 файла-реализации, 1 test suite и 1 API dump. Доминируют два контекста: dataflow/liveness analysis в компиляторе и character class operations в regex-движке.

## Методология

Локальный поиск по репозиторию `/Users/dmitry.nekrasov/dev/repos/kotlin`:

1. `grep -rl "BitSet" --include="*.kt" --include="*.java"` — основной поиск прямых упоминаний (24 файла).
2. `grep -r "import java.util.BitSet" --include="*.kt" --include="*.java"` — файлы с явным импортом (4 файла).
3. `grep -r "import java\.util\.\*" --include="*.kt" --include="*.java"` + проверка использования BitSet в теле — файлы с wildcard-импортом (9 файлов).
4. `grep -Erl "\.forEachBit\s*\{|\.mapEachBit\s*\{" --include="*.kt"` — потребители `forEachBit`/`mapEachBit` по фактическому вызову метода (а не по import-строке, которая может быть unused). Вычитание файлов, уже найденных на этапе 1, даёт 1 дополнительный файл: `CallGraphBuilder.kt` — direct consumer `CustomBitSet` без literal "BitSet" в тексте.
5. `grep -Er "class.*BitSet|BitSet\(|bitSet\b" --include="*.kt" --include="*.java"` — проверка на BitSet-подобные конструкции.
6. `grep -Er "bitMask|bitmask|BitMask" --include="*.kt" --include="*.java"` — проверка на альтернативные паттерны (17 файлов с bitmask-операциями на Int/Long — это не BitSet-структуры, исключены из каталога).

**Критерий включения:** в каталог входят файлы, которые (a) содержат прямое упоминание типа `BitSet`/`CustomBitSet` (через `import`, определение или вызов), либо (b) импортируют и вызывают BitSet API (extension-функции из `BitSetUtil.kt` или методы `CustomBitSet`), даже если слово "BitSet" не встречается в тексте файла. Файлы, которые обращаются к BitSet-функциональности исключительно через абстракцию более высокого уровня (например, `AbstractCharClass.intersects()`), не включаются — они являются потребителями абстракции, а не BitSet API.

**Входные данные:** `bitset-research/step-01-kotlin-implementations.md` — список известных внутренних реализаций (kotlin.native.BitSet, CustomBitSet, Wasm BitSet, BitSetUtil.kt).

---

## Каталог использований

### 1. Compiler: Utility Extensions (BitSetUtil.kt)

#### compiler/util/src/org/jetbrains/kotlin/utils/BitSetUtil.kt

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet`
**Методы:** Определяет extension-функции, а не использует BitSet напрямую. Внутри вызывает: `BitSet(size)` (конструктор), `.size()`, `.or()`, `.nextSetBit()`.
**Контекст:** Предоставляет три extension-функции для `java.util.BitSet`: `copy()` (типизированное клонирование через `BitSet(this.size()).apply { this.or(this@copy) }`), `forEachBit {}` (inline-итерация по set-битам через `nextSetBit`), `mapEachBit {}` (map по set-битам в `List<R>`). Компенсирует отсутствие удобной итерации и типизированного копирования в Java API. Подробный анализ — `step-01-kotlin-implementations.md`, секция 4.2.

---

### 2. Compiler: PSI/Parser

#### compiler/psi/parser/src/org/jetbrains/kotlin/kdoc/lexer/_KDocLexer.java

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet;`
**Методы:** `new BitSet(int)`, `.set(int, boolean)`, `.clear(int)`, `.get(int)`, `.size()`
**Контекст:** Сгенерированный JFlex-лексер для KDoc. BitSet используется как поле `zzFin` для отслеживания accepting states в backwards DFA при обработке general lookahead statements. Каждый бит соответствует позиции символа в буфере.

---

### 3. Compiler: JVM Backend

#### compiler/backend/src/org/jetbrains/kotlin/codegen/coroutines/resumePointDependentAnalysis.kt

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet`, `import org.jetbrains.kotlin.utils.copy`, `import org.jetbrains.kotlin.utils.forEachBit`
**Методы:** `BitSet(int)`, `.set(int)`, `[int]` (operator get), `.clear()`, `.or()`, `.copy()` (ext), `.forEachBit {}` (ext), `!=` / `.equals()`, `.toString()`
**Контекст:** Два экземпляра BitSet:
- `visited`: множество посещённых instruction indices при reverse DFS — поиск путей между suspension points, где локальная переменная не инициализирована.
- `isAfterSuspensionPoint`: per-frame BitSet, где каждый бит — suspension point; бит `i` установлен, если текущая инструкция достижима после resume из suspension point `i`. Используется для dataflow analysis: определение, какие local variable slots нужно реинициализировать при resume корутины.

#### compiler/backend/src/org/jetbrains/kotlin/codegen/optimization/common/variableLiveness.kt

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.*`
**Методы:** `BitSet(int)`, `.or()`, `.set(int, boolean)`, `.get(int)`, `[int]` (operator get), `.equals()`, `.hashCode()`
**Контекст:** Класс `VariableLivenessFrame` оборачивает `BitSet` для отслеживания живых локальных переменных при backward liveness analysis JVM-байткода. Каждый бит соответствует слоту локальной переменной. `.or()` используется для merge состояний при слиянии control-flow; `.set(index, true/false)` — при пометке переменной как alive/dead.

#### compiler/backend/src/org/jetbrains/kotlin/codegen/optimization/boxing/PopBackwardPropagationTransformer.kt

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.*`
**Методы:** `BitSet(int)`, `[int] = true` (operator set), `[int]` (operator get)
**Контекст:** `dontTouchInsnIndices: BitSet` — множество индексов байткод-инструкций, которые нельзя удалять при оптимизации POP backward propagation. Инструкции, чьи выходные значения потребляются другими non-dead инструкциями, помечаются в BitSet; при финальном проходе BitSet проверяется для принятия решения о сохранении инструкции.

---

### 4. Compiler: Common IR Backend

#### compiler/ir/backend.common/src/org/jetbrains/kotlin/backend/common/lower/optimizations/LivenessAnalysis.kt

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.*`, `import org.jetbrains.kotlin.utils.copy`, `import org.jetbrains.kotlin.utils.forEachBit`
**Методы:** `BitSet()`, `.copy()` (ext), `.forEachBit {}` (ext), `.or()`, `.andNot()`, `.set(int)`, `.get(int)`, `.clear(int)`
**Дополнительно — приватные extensions:**
- `fun BitSet.withBit(bit): BitSet` — `if (get(bit)) this else copy().also { it.set(bit) }` — immutable-style добавление бита.
- `fun BitSet.withOutBit(bit): BitSet` — `if (!get(bit)) this else copy().also { it.clear(bit) }` — immutable-style удаление бита.
- `fun BitSet.format()` — debug-форматирование (unused, помечено `@Suppress("unused")`).
**Контекст:** Backward liveness analysis на уровне IR. Каждый бит — `IrVariable`. `BitSet` используется как тип данных и результата в `IrVisitor<BitSet, BitSet>`. Множества живых переменных передаются через visitor pattern от конца функции к началу. `withBit`/`withOutBit` реализуют copy-on-write семантику: при `IrGetValue` бит добавляется (переменная жива), при `IrSetValue`/`IrVariable` — удаляется (переменная определяется). `.or()` — для merge в join-точках control flow. Цикл анализа при обработке loops выполняется до насыщения (fixed-point iteration).

---

### 5. Compiler: K/N Backend (java.util.BitSet)

#### kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/optimizations/ComputeTypesPass.kt

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet`, `import org.jetbrains.kotlin.utils.copy`, `import org.jetbrains.kotlin.utils.forEachBit`, `import org.jetbrains.kotlin.utils.mapEachBit`
**Методы:** `BitSet()`, `.set(int)`, `.and()`, `.or()`, `.andNot()`, `.copy()` (ext), `.mapEachBit {}` (ext), `.forEachBit {}` (ext)
**Контекст:** Forward dataflow analysis для вычисления типов IR-переменных в K/N backend. Каждый бит представляет запись (write) в переменную. BitSet используется как тип данных в `IrVisitor<BitSet, BitSet>` и хранится в `Map<IrVariable, BitSet>` / `Map<IrGetValue, BitSet>`. `.and()` / `.or()` — для merge при join-точках; `.mapEachBit {}` — для сбора типов из записей.

#### kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/optimizations/StaticInitializersOptimization.kt

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.*`, `import org.jetbrains.kotlin.utils.copy`
**Методы:** `BitSet()`, `.or()`, `.and()`, `.copy()` (ext), `.set(int)`, `.get(int)`, `.cardinality()`
**Дополнительно — приватная extension:**
- `fun BitSet.withSetBit(bit): BitSet` — `if (this.get(bit)) this else copy().also { it.set(bit) }` — аналог `withBit` из LivenessAnalysis.
**Контекст:** Анализ для оптимизации вызовов static initializers в K/N backend. Каждый бит соответствует `IrDeclarationContainer`. BitSet отслеживает, какие контейнеры гарантированно инициализированы в каждой точке программы. `IrVisitor<BitSet, BitSet>` — входящий параметр и результат: множества инициализированных контейнеров до/после каждого IR-элемента. `.and()` — для пересечения при merge веток; `.or()` — для объединения с гарантированно инициализированными globals/thread-locals; `.withSetBit()` — для пометки контейнера как инициализированного после вызова его static initializer. `.cardinality()` — подсчёт суммарного размера множеств инициализированных контейнеров как метрики сходимости при межпроцедурном fixed-point iteration.

---

### 6. Compiler: K/N Backend (CustomBitSet)

> Реализация `CustomBitSet` подробно проанализирована в `step-01-kotlin-implementations.md`, секция 4.1.

#### kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/optimizations/DataFlowIR.kt

**BitSet type:** `CustomBitSet`
**Import:** `import org.jetbrains.kotlin.backend.konan.util.CustomBitSet`
**Методы:** `CustomBitSet()`, `.set(int)`, `[int]` (operator get), `.isEmpty`, `.or()`
**Контекст:** Класс `TypeHierarchy` использует `CustomBitSet` для хранения множеств наследников каждого типа. `allInheritors: Array<CustomBitSet>` — массив, где `allInheritors[typeId]` содержит set-биты для всех наследников типа `typeId`. Метод `inheritorsOf()` строит множество рекурсивно через `.or()` из наследников подтипов. `visited: CustomBitSet` предотвращает повторную обработку типов.

#### kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/optimizations/DevirtualizationAnalysis.kt

**BitSet type:** `CustomBitSet`
**Import:** `import org.jetbrains.kotlin.backend.konan.util.CustomBitSet`
**Методы:** `CustomBitSet()`, `CustomBitSet(nodesCount)`, `.set(int)`, `[int]` (operator get), `[int] = value` (operator set), `.clear()`, `.cardinality()`, `.forEachBit {}`, `.copy()`, `.or()`, `.and()`, `.equals()`, `orWithFilterHasChanged()`
**Дополнительно:**
- `fun CustomBitSet.format(allTypes)` — extension для debug-печати типов через `.forEachBit {}` и `[it.index]`.
- `inline fun forEachBitInBoth(first, second, block)` — custom-функция, итерирующая по пересечению двух CustomBitSet через `.cardinality()` и `.forEachBit {}`.
**Контекст:** Ядро devirtualization analysis в K/N backend. Самый тяжёлый потребитель CustomBitSet в репозитории. Множества типов (`Node.types: CustomBitSet`) представляют возможные runtime-типы значений. `instantiatingClasses: CustomBitSet` — множество типов, для которых существуют конструкторы. `visited`, `processedVirtualNodes`, `usefulNodes`, `marked`, `potentialExternalVirtualCall` — различные worklist-множества. `castEdges`, `seenBitSetTo`, `suitableTypes` — массивы CustomBitSet для фильтрации типов при cast edges. `.orWithFilterHasChanged()` — критична для fixed-point iteration (dataflow analysis) для определения момента сходимости. `.clear()` — переиспользование рабочих marker-set'ов (например, `visited.clear()` между проходами). `.equals()` — схлопывание эквивалентных cast edges (`tp?.equals(it.suitableTypes)`).

#### kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/optimizations/CastsOptimization.kt

**BitSet type:** `CustomBitSet`
**Import:** `import org.jetbrains.kotlin.backend.konan.util.CustomBitSet`
**Методы:** `CustomBitSet()`, `CustomBitSet.valueOf(LongArray)`, `.set(int)`, `.cardinality()`, `.intersects()`, `.copy()`, `.or()`, `.andNot()`, `.forEachBit {}`, `.forEachWord {}`, `.size` (свойство — word count)
**Контекст:** Оптимизация cast'ов в K/N backend. `Disjunction(val terms: CustomBitSet)` — дизъюнкция термов, где каждый бит в CustomBitSet представляет терм. `Conjunction` — список `Disjunction`. Используется для булевой оптимизации предикатов типов. `.forEachWord {}` — прямой доступ к raw-словам для операции `someTermBothTrueAndFalse` (проверка, что бит и его инверсия оба присутствуют). `invertTerms()` — инверсия термов через `valueOf(LongArray)`. `.forEachBit {}` — итерация по термам при разложении предиката.

#### kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/optimizations/CallGraphBuilder.kt

**BitSet type:** `CustomBitSet` (косвенно, через результат `typeHierarchy.inheritorsOf()`)
**Import:** `import org.jetbrains.kotlin.utils.forEachBit` (extension для java.util.BitSet, но вызывается на CustomBitSet, который имеет собственный `forEachBit`)
**Методы:** `.forEachBit {}`
**Контекст:** При построении call graph для devirtualization вызывает `typeHierarchy.inheritorsOf(call.receiverType).forEachBit { ... }` для итерации по всем возможным наследникам типа receiver'а виртуального вызова. Для каждого наследника ищет конкретную реализацию виртуального метода.

> **Примечание:** файл импортирует `org.jetbrains.kotlin.utils.forEachBit` (extension для `java.util.BitSet`), но `inheritorsOf()` возвращает `CustomBitSet`, у которого есть собственный `forEachBit`. Вероятно, импорт не используется (unused import) — вызывается метод экземпляра `CustomBitSet.forEachBit`, а не extension-функция. Файл не содержит слова "BitSet" и не обнаруживается через `grep -rl "BitSet"`.

---

### 7. Analysis: Symbol Light Classes

#### analysis/symbol-light-classes/src/org/jetbrains/kotlin/light/classes/symbol/classes/symbolLightClassUtils.kt

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.*`
**Методы:** `BitSet(int)`, `.set(int)`, `.set(from, to)`, `.isEmpty`, `.clear(int)`, `.get(int)` / `[int]` (operator get), `.copy()` (ext из symbolLightUtils.kt), `.intersects()`, `.length()`, `clone() as BitSet`, `.equals()` / `.hashCode()` (неявно через `Set<BitSet>.contains()`)
**Дополнительно — приватная inline extension:**
- `fun BitSet.copyAndModify(block): BitSet` — `clone() as BitSet` + apply block.
**Контекст:** BitSet используется как "value parameter pick mask" для генерации JVM overloads при `@JvmOverloads`. Каждый бит — индекс параметра функции; set-бит означает, что параметр включён в данный overload. `symbolLightClassUtils` создаёт маски через `BitSet(parameterCount)`, затем `.set(0, parameterCount)` для полной маски. `.clear(index)` убирает параметры при генерации overloads от конца к началу. `Set<BitSet>` (`deprecatedMasks`) хранит deprecated комбинации параметров (через `@IntroducedAt`); `.intersects()` проверяет конфликты; `.length()` определяет диапазон значимых битов. `copyAndModify {}` — immutable-style создание новых масок.

#### analysis/symbol-light-classes/src/org/jetbrains/kotlin/light/classes/symbol/symbolLightUtils.kt

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.*`
**Методы:** `clone() as BitSet`
**Контекст:** Определяет единственную extension-функцию `internal fun BitSet.copy(): BitSet = clone() as BitSet`. Потребляется `symbolLightClassUtils.kt`. Аналог `copy()` из `BitSetUtil.kt`, но реализованный через `clone()` вместо `BitSet(size).apply { or(this@copy) }`.

#### analysis/symbol-light-classes/src/org/jetbrains/kotlin/light/classes/symbol/methods/SymbolLightMethod.kt

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.*`
**Методы:** `.get(int)`, `.equals()` (неявно, через `!=` в `equals()`)
**Контекст:** Хранит `valueParameterPickMask: BitSet?` как свойство. При построении списка параметров light-метода проверяет `valueParameterPickMask?.get(index) == false` для пропуска параметра. В `equals()` сравнивает маски через `!=`.

#### analysis/symbol-light-classes/src/org/jetbrains/kotlin/light/classes/symbol/methods/SymbolLightSimpleMethod.kt

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.*`
**Методы:** (pass-through — не вызывает методы BitSet напрямую)
**Контекст:** Принимает `valueParameterPickMask: BitSet?` как параметр конструктора и передаёт в родительский `SymbolLightMethod`. Companion-объект `createSimpleMethods` — фабрика, создающая экземпляры с соответствующей маской.

#### analysis/symbol-light-classes/src/org/jetbrains/kotlin/light/classes/symbol/methods/SymbolLightConstructor.kt

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.*`
**Методы:** (pass-through — не вызывает методы BitSet напрямую, передаёт в `hasValueClassInSignature()`)
**Контекст:** Принимает `valueParameterPickMask: BitSet?` (default `null`) и передаёт в `SymbolLightMethod`. В `computeModifiers` передаёт маску в `hasValueClassInSignature()` для определения, нужно ли делать конструктор private из-за value class mangling.

---

### 8. Stdlib: Wasm/Native Regex Engine

> Реализации Wasm internal BitSet и expect-декларация подробно проанализированы в `step-01-kotlin-implementations.md`, секции 3 и соответствующие части.

#### libraries/stdlib/native-wasm/src/kotlin/text/regex/CharClass.kt

**BitSet type:** `kotlin.native.BitSet`
**Import:** `import kotlin.native.BitSet`
**Методы:** `BitSet()`, `.set(int, Boolean)`, `.set(from, to, Boolean)`, `.get(int)` / `[int]` (operator get), `.xor()`, `.and()`, `.or()`, `.andNot()`, `.isEmpty`, `.nextSetBit()`
**Контекст:** Основная структура данных для character classes в regex-движке Native/Wasm таргетов. Каждый `CharClass` содержит `bits_: BitSet`, где каждый бит соответствует code point символа. Битовые операции реализуют теоретико-множественные операции над character classes:
- `.or()` — union (объединение классов)
- `.and()` — intersection (пересечение)
- `.xor()` — symmetric difference
- `.andNot()` — subtraction (вычитание)
- `.nextSetBit()` — итерация по символам класса при генерации строкового представления.
Методы `union()`, `intersection()`, `add()` и другие активно используют bulk-операции.

#### libraries/stdlib/native-wasm/src/kotlin/text/regex/AbstractCharClass.kt

**BitSet type:** `kotlin.native.BitSet`
**Import:** `import kotlin.native.BitSet`
**Методы:** `BitSet(int)`, `.nextClearBit()`, `.nextSetBit()`, `[int]` (operator get), `.set(IntRange)`, `.set(from, to)`, `.intersects()`
**Контекст:** Абстрактный базовый класс для character classes. Поле `lowHighSurrogates: BitSet(SURROGATE_CARDINALITY)` отслеживает, какие unpaired surrogate символы включены в класс. `.nextSetBit(0)` / `.nextClearBit(0)` — проверка наличия surrogates. `[index]` — проверка принадлежности символа при matching. `.set(range)` и `.set(from, to)` — установка диапазонов surrogate символов. В companion helper `intersects(cc1, cc2)` делегирует проверку пересечения двух character classes через `cc1.bits!!.intersects(cc2.bits!!)`.

---

### 9. Native: Commonizer

#### native/commonizer/src/org/jetbrains/kotlin/commonizer/stats/RawStatsCollector.kt

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.*`
**Методы:** `BitSet(int)` (через typealias `StatsValue`), `[int] = true` (operator set), `[int]` (operator get)
**Контекст:** `private typealias StatsValue = BitSet`. Используется для отслеживания, на каких target-платформах присутствует данная декларация при commonization. Каждый бит — target-платформа (плюс один бит для "common"). `statsValue[targetIndex] = true` — пометка присутствия; `statsValue[index]` — проверка при генерации статистики (lifted, expected/actual, missing).

---

### 10. Внутренние реализации BitSet

> Подробный анализ всех реализаций — `step-01-kotlin-implementations.md`.

#### kotlin-native/runtime/src/main/kotlin/kotlin/native/BitSet.kt

**Тип:** `kotlin.native.BitSet` (actual-реализация для Native)
**Определяемые методы:** `BitSet(size)`, `BitSet(length, initializer)`, `.set(Int, Boolean)`, `.set(from, to, Boolean)`, `.set(IntRange, Boolean)`, `.clear(Int)`, `.clear(from, to)`, `.clear(IntRange)`, `.clear()`, `.flip(Int)`, `.flip(from, to)`, `.flip(IntRange)`, `operator get(Int)`, `.nextSetBit(startIndex)`, `.nextClearBit(startIndex)`, `.previousBit(startIndex, Boolean)`, `.previousSetBit(startIndex)`, `.previousClearBit(startIndex)`, `.and()`, `.or()`, `.xor()`, `.andNot()`, `.intersects()`, `.isEmpty`, `.lastTrueIndex`, `.size`, `.toString()`, `.hashCode()`, `.equals()`
**Контекст:** Полноценная реализация растущего BitSet на `LongArray`. Публичный API в пакете `kotlin.native`, помечен `@ObsoleteNativeApi`. Подробный анализ — `step-01-kotlin-implementations.md`, секция 2.

#### kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/util/CustomBitSet.kt

**Тип:** `CustomBitSet` (internal)
**Определяемые методы:** `CustomBitSet(size)`, `CustomBitSet.valueOf(LongArray)`, `.set(Int)`, `.clear(Int)`, `.clear()`, `operator get(Int)`, `operator set(Int, Boolean)`, `.cardinality()`, `.forEachBit {}` (inline), `.forEachWord {}` (inline), `.or()`, `.orWithFilterHasChanged()` (2 варианта), `.and()`, `.andNot()`, `.intersects()`, `operator contains()`, `.copy()`, `.isEmpty`, `.size`, `.hashCode()`, `.hashCodeLong()`
**Контекст:** Высокопроизводительная internal-реализация для devirtualization и dataflow analysis в K/N backend. Оптимизирована для tight loops: `forEachBit` через `countTrailingZeroBits`, `orWithFilterHasChanged` для fixed-point iteration. Подробный анализ — `step-01-kotlin-implementations.md`, секция 4.1.

#### libraries/stdlib/wasm/src/kotlin/text/regex/BitSet.kt

**Тип:** Wasm internal BitSet (`actual` для `kotlin.native.BitSet` expect)
**Определяемые методы:** `BitSet(size)`, `.set(Int, Boolean)`, `.set(from, to, Boolean)`, `.set(IntRange, Boolean)`, `operator get(Int)`, `.nextSetBit(startIndex)`, `.nextClearBit(startIndex)`, `.and()`, `.or()`, `.xor()`, `.andNot()`, `.intersects()`, `.isEmpty`, `.size`
**Контекст:** Stripped-down копия K/N `BitSet` для Wasm-таргета. Используется исключительно в regex-движке (`CharClass.kt`, `AbstractCharClass.kt`). Подробный анализ — `step-01-kotlin-implementations.md`, секция 3.

#### libraries/stdlib/native-wasm/src/kotlin/native/BitSet.kt

**Тип:** `kotlin.native.BitSet` (expect-декларация)
**Определяемые методы:** (expect) `BitSet(size)`, `.set(Int, Boolean)`, `.set(from, to, Boolean)`, `.set(IntRange, Boolean)`, `.nextSetBit(startIndex)`, `.nextClearBit(startIndex)`, `operator get(Int)`, `.and()`, `.or()`, `.xor()`, `.andNot()`, `.intersects()`, `.isEmpty`, `.size`
**Контекст:** Общая expect-декларация для Native и Wasm actual-реализаций. Определяет минимальный контракт, необходимый regex-движку. Помечена `@ObsoleteNativeApi` и `internal`. Подробный анализ — `step-01-kotlin-implementations.md`, секция 3.

---

### 11. Тесты

#### kotlin-native/runtime/test/collections/BitSetTest.kt

**BitSet type:** `kotlin.native.BitSet`
**Тестируемые методы:** `BitSet()`, `BitSet(size)`, `BitSet(size) { predicate }`, `.set(int)`, `.set(int, Boolean)`, `.set(range)`, `.set(range, Boolean)`, `.set(from, to)`, `.set(from, to, Boolean)`, `.clear(int)`, `.clear(range)`, `.clear(from, to)`, `.clear()`, `[int]` (operator get), `.flip(int)`, `.flip(range)`, `.flip(from, to)`, `.isEmpty`, `.lastTrueIndex`, `.nextSetBit()`, `.nextSetBit(startIndex)`, `.nextClearBit()`, `.nextClearBit(startIndex)`, `.previousSetBit()`, `.previousClearBit()`, `.and()`, `.or()`, `.xor()`, `.andNot()`, `.intersects()`, `.equals()`, `.hashCode()`
**Контекст:** Комплексный test suite для `kotlin.native.BitSet`. Покрывает конструкторы, single-bit и range-операции set/clear/flip, навигацию (next/previous set/clear bit), bulk-операции (and, or, xor, andNot), intersects, equality, hash codes. Также проверяет `IndexOutOfBoundsException` для отрицательных индексов.

---

### 12. API Dump

#### libraries/tools/binary-compatibility-validator/klib-public-api/kotlin-stdlib.api

**Контекст:** Public API dump для `kotlin.native.BitSet` (klib-формат). Содержит полную подпись всех публичных членов: конструкторы, свойства (`isEmpty`, `lastTrueIndex`, `size`), методы (and, andNot, clear, equals, flip, get, hashCode, intersects, nextClearBit, nextSetBit, or, previousBit, previousClearBit, previousSetBit, set, toString, xor), Companion object. Используется для контроля бинарной совместимости. Не является местом использования — фиксирует публичный контракт.

---

## Полнота

**Этап 1 — literal-hit файлы:**

```bash
grep -rl "BitSet" --include="*.kt" --include="*.java" --include="*.api" --exclude-dir=bitset-research | sort
```

**Результат:** 25 файлов. Все 25 присутствуют в каталоге (24 `.kt`/`.java` + 1 `.api` — `kotlin-stdlib.api`).

**Этап 2 — косвенные use-site'ы (direct API consumers без literal "BitSet"):**

Поиск по фактическому вызову метода (а не по import-строке, которая может быть unused):

```bash
grep -Erl "\.forEachBit\s*\{|\.mapEachBit\s*\{" --include="*.kt" --exclude-dir=bitset-research | sort
```

**Результат:** 6 файлов, из которых 5 уже входят в базовый список (этап 1). Единственный дополнительный файл — `CallGraphBuilder.kt`: вызывает `.forEachBit {}` на `CustomBitSet` (через `typeHierarchy.inheritorsOf(...).forEachBit { ... }`), но не содержит слова "BitSet". Именно этот второй этап необходим для обнаружения direct consumer'ов `CustomBitSet`, у которого `forEachBit` — метод экземпляра, не требующий `import` с упоминанием "BitSet".

**Канонический итог:** 25 literal-hit файлов + 1 дополнительный direct API consumer = **26 каталогизированных записей** (20 use-site'ов + 4 реализации + 1 test suite + 1 API dump).

**Разбивка по BitSet-типам (только use-site'ы):**
- `java.util.BitSet`: 14 файлов (секции 1–5, 7, 9)
- `CustomBitSet`: 4 файла (секция 6)
- `kotlin.native.BitSet`: 2 файла (секция 8)
