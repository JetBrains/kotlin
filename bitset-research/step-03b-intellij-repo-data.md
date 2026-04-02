# Шаг 3b. Каталог использования BitSet в репозитории IntelliJ Community

## Резюме

Каталогизированы все 225 файлов в репозитории intellij-community, содержащих упоминания BitSet (`grep -rl "BitSet"`): 211 исходных файлов (`.java`/`.kt`) и 14 не-source файлов (API dumps, JDK API version files, JDK-аннотации, тестовые данные). Исходные файлы организованы по подсистемам: platform/util (28), plugins/java-decompiler (27), platform/diff-impl (19), platform/vcs-log (19), python (16, из них 13 сгенерированных), plugins/groovy (11), platform/vcs-impl (11), platform/lang-impl (10), java/java-analysis-impl (10), platform/analysis-impl (7), и ещё ~20 подсистем с 1-5 файлами. Обнаружено 11 кастомных BitSet-реализаций/обёрток (standalone типы с BitSet-centric именем или API; локальные наследники вроде `MarkerOptionalData extends BitSet` не считаются): `ConcurrentBitSet`, `ConcurrentThreeStateBitSet`, `BitSetAsRAIntContainer`, `IdBitSet`, `com.intellij.util.diff.BitSet` (копия K/N stdlib), BitSet typealias (text-matching), `MutableBitSet`/`BitSet` (syntax), `UnsignedBitSet`, `BitSetFlags`, `BitSet32` (mslinks), `fleet.util.BitSet` (производная копия K/N stdlib). Один файл (`text-matching/BitSet.kt`) содержит прямую ссылку на задачу KT-55163 — мультиплатформенный BitSet для Kotlin stdlib. Файл `util/diff/BitSet.kt` — копия K/N stdlib, но прямой ссылки на KT-55163 не содержит. Сырой каталог без классификации — анализ будет выполнен в шаге 3c.

## Методология

Локальный поиск по репозиторию `/Users/dmitry.nekrasov/dev/repos/intellij-community`:

1. `grep -rl "BitSet" --include="*.java" --include="*.kt"` — поиск исходных файлов (211 файлов).
2. `grep -rl --exclude-dir=.git --binary-files=without-match "BitSet"` — расширенный поиск по всем текстовым файлам (225 файлов, +14 не-source: API dumps, JDK API version files, аннотации, тестовые данные).
3. Группировка по подсистемам (top-level directory + second-level) через `cut -d'/' -f1-2 | sort | uniq -c | sort -rn`.
4. Для каждого файла — чтение, определение типа BitSet (java.util.BitSet, ConcurrentBitSet, com.intellij.util.diff.BitSet и др.), стиля импорта, вызываемых методов и контекста.
5. Кастомные реализации — расширенное описание с полным API surface.
6. Сгенерированный код (Thrift, JFlex) — condensed формат: паттерн описан один раз, затем список файлов.

**Критерий включения:** файл содержит прямое упоминание типа `BitSet` (через import, определение, вызов, или упоминание полностью квалифицированного имени). Файлы, содержащие `BitSet` только как строковую константу (например, `"toBitSet"` в `StreamExCallChecker.kt`) или как имя переменной/метода `TokenSet` (например, `ClassElement.java` с `MODIFIERS_TO_REMOVE_IN_INTERFACE_BIT_SET: TokenSet`), отмечены как false positives.

---

## Каталог использований

### 1. Platform: Util — Concurrency (ConcurrentBitSet, ConcurrentThreeStateBitSet)

#### platform/util/concurrency/src/com/intellij/util/containers/ConcurrentBitSet.java

**Тип:** Кастомная реализация (интерфейс)
**Определяемые методы:** `create()`, `create(int)`, `set(int)` -> boolean, `set(int, boolean)`, `clear(int)` -> boolean, `clear()`, `get(int)`, `nextSetBit(int)`, `nextClearBit(int)`, `size()`, `cardinality()`, `toIntArray()`, `readFrom(DataInputStream)`
**Внутреннее хранилище:** Определяет контракт; реализация в `ConcurrentBitSetImpl`
**Контекст:** Потокобезопасный аналог `java.util.BitSet`. Оптимизирован для сценариев с частым чтением и редкой записью. Агрегирующие методы (cardinality, nextSetBit и т.д.) дают транзиентные результаты в многопоточном окружении. Не поддерживает `flip()` — требуется идемпотентность всех операций.

---

#### platform/util/concurrency/src/com/intellij/util/containers/ConcurrentBitSetImpl.java

**Тип:** Кастомная реализация
**Определяемые методы:** `ConcurrentBitSetImpl()`, `ConcurrentBitSetImpl(int)`, `ConcurrentBitSetImpl(int[])`, `set(int)`, `set(int, boolean)`, `clear(int)`, `clear()`, `get(int)`, `nextSetBit(int)`, `nextClearBit(int)`, `size()`, `cardinality()`, `toIntArray()`, `toString()`, `readFrom(File)`, `changeWord(int, IntUnaryOperator)` (package-private), `getWord(int)` (package-private)
**Внутреннее хранилище:** `volatile int[] array` + `VarHandle` (MethodHandles.arrayElementVarHandle) для атомарного доступа к элементам. 32 бита на элемент массива.
**Контекст:** Реализация `ConcurrentBitSet`. Биты хранятся в `int[]`, по 32 бита на элемент. При изменении бита массив при необходимости реаллоцируется (внутри `synchronized`). Все мутирующие операции должны быть идемпотентными. Использует `VarHandle.setVolatile / getVolatile` для записи/чтения отдельных слов.

---

#### platform/util/concurrency/src/com/intellij/util/containers/ConcurrentPackedBitsArray.java

**Тип:** Кастомная реализация (интерфейс)
**Определяемые методы:** `create(int bitsPerChunk)`, `get(int id)` -> long, `set(int id, long flags)` -> long, `clear()`
**Внутреннее хранилище:** Определяет контракт; реализация в `ConcurrentPackedBitsArrayImpl`
**Контекст:** Пакует заданное число бит (1..32) в чанки, хранит их атомарно в массиве. Полезен для хранения связанных флагов вместе. Гарантии аналогичны `ConcurrentBitSet`, но для чанков вместо отдельных бит.

---

#### platform/util/concurrency/src/com/intellij/util/containers/ConcurrentPackedBitsArrayImpl.java

**Тип:** Кастомная реализация
**Определяемые методы:** `ConcurrentPackedBitsArrayImpl(int bitsPerChunk)`, `get(int id)`, `set(int id, long flags)`, `clear()`
**Внутреннее хранилище:** Внутри использует `ConcurrentBitSetImpl` — делегирует хранение бит экземпляру `ConcurrentBitSetImpl`. Маска и число чанков на слово вычисляются из `bitsPerChunk`.
**Контекст:** Реализация `ConcurrentPackedBitsArray`. Не является BitSet в прямом смысле, но надстроена над `ConcurrentBitSetImpl`, используя его `changeWord()` и `getWord()` для атомарного чтения/записи чанков бит.

---

#### platform/util/concurrency/src/com/intellij/util/containers/ConcurrentThreeStateBitSet.kt

**Тип:** Кастомная реализация (интерфейс, Kotlin)
**Определяемые методы:** `create()`, `create(Int)`, `set(Int, Boolean?)`, `get(Int)` -> Boolean?, `compareAndSet(Int, Boolean?, Boolean?)`, `clear()`, `size()`
**Внутреннее хранилище:** Определяет контракт; реализация в `ConcurrentThreeStateBitSetImpl`
**Контекст:** Потокобезопасный BitSet с поддержкой трёх состояний: `true`, `false`, `null` (не установлен). На основе `ConcurrentBitSet`. Полезен для кешей, где нужно различать «не вычислено» от «вычислено как false».

---

#### platform/util/concurrency/src/com/intellij/util/containers/ConcurrentThreeStateBitSetImpl.kt

**Тип:** Кастомная реализация (Kotlin, internal)
**Определяемые методы:** `set(Int, Boolean?)`, `get(Int)` -> Boolean?, `compareAndSet(Int, Boolean?, Boolean?)`, `clear()`, `size()`
**Внутреннее хранилище:** Внутри использует `ConcurrentBitSet.create(estimatedSize * 2)`. Для каждого логического бита хранит 2 физических: status-бит (`bitIndex * 2`) и value-бит (`bitIndex * 2 + 1`). Если status=false, значение считается `null`.
**Контекст:** Реализация трёхсостояния через удвоение бит в `ConcurrentBitSet`. Мутирующие операции (`set`, `compareAndSet`, `clear`) обёрнуты в `synchronized(this)`; `get()` и `size()` читают underlying `ConcurrentBitSet` без синхронизации.

---

### 2. Platform: Util — Diff (Custom BitSet)

#### platform/util/diff/src/com/intellij/util/diff/BitSet.kt

**Тип:** Кастомная реализация (Kotlin, `@ApiStatus.Internal`)
**Определяемые методы:** `BitSet(Int)`, `BitSet(Int, (Int) -> Boolean)`, `BitSet(Int, LongArray)`, `set(Int, Boolean)`, `set(Int, Int, Boolean)`, `set(IntRange, Boolean)`, `clear(Int)`, `clear(Int, Int)`, `clear(IntRange)`, `clear()`, `flip(Int)`, `flip(Int, Int)`, `flip(IntRange)`, `nextSetBit(Int)`, `nextClearBit(Int)`, `previousBit(Int, Boolean)`, `previousSetBit(Int)`, `previousClearBit(Int)`, `get(Int)`, `and(BitSet)`, `or(BitSet)`, `xor(BitSet)`, `andNot(BitSet)`, `intersects(BitSet)`, `toLongArray()`, `cardinality()`, `toString()`, `hashCode()`, `equals(Any?)`
**Свойства:** `lastTrueIndex`, `isEmpty`, `size`
**Внутреннее хранилище:** `LongArray` (64 бита на элемент). Автоматически расширяется при необходимости.
**Контекст:** Скопировано из Kotlin/Native stdlib (комментарий в коде). Полнофункциональная мультиплатформенно-совместимая реализация BitSet. Используется во внутренней diff-подсистеме IntelliJ вместо `java.util.BitSet` — вероятно, для мультиплатформенной совместимости модуля diff. Поддерживает побитовые операции (and, or, xor, andNot), навигацию (next/previous set/clear bit), диапазонные операции.

---

#### platform/util/diff/src/com/intellij/util/diff/Diff.kt

**BitSet type:** `com.intellij.util.diff.BitSet` (кастомный)
**Import:** Нет явного импорта — находится в том же пакете.
**Методы:** Объявляет переменную `var changes: Array<BitSet>?`; передаёт в `PatienceIntLCS` / `MyersLCS`, получает обратно `patienceIntLCS.changes` / `intLCS.changes`. Передаёт в `reindexer.reindex(changes, builder)`.
**Контекст:** Основной класс diff-алгоритма. Использует кастомный `BitSet` как массив из двух элементов `Array<BitSet>` для хранения изменений: `changes[0]` — изменённые позиции в первой последовательности, `changes[1]` — во второй.

---

#### platform/util/diff/src/com/intellij/util/diff/MyersLCS.kt

**BitSet type:** `com.intellij.util.diff.BitSet` (кастомный)
**Import:** Нет явного импорта — тот же пакет.
**Методы:** `BitSet(Int)` (конструктор), `.set(Int, Int, Boolean)` (range set), `changes` property -> `Array<BitSet>`
**Контекст:** Алгоритм Myers для поиска наибольшей общей подпоследовательности (LCS). Принимает `changes1: BitSet` и `changes2: BitSet` в конструкторе. Инициализирует все биты в range `[start, start+count)` как true (изменены), затем сбрасывает совпавшие позиции через `set(range, false)` в `addUnchanged()`.

---

#### platform/util/diff/src/com/intellij/util/diff/PatienceIntLCS.kt

**BitSet type:** `com.intellij.util.diff.BitSet` (кастомный)
**Import:** Нет явного импорта — тот же пакет.
**Методы:** `BitSet(Int)` (конструктор), `.set(Int, Int)` (range set), `changes` property -> `Array<BitSet>`
**Контекст:** Patience Diff алгоритм для LCS. Аналогичная схема: принимает 2 BitSet-а в конструкторе, помечает изменённые диапазоны через `addChange()` -> `changes1.set(start1, start1 + count1)`. Делегирует MyersLCS для внутренних чанков.

---

#### platform/util/diff/src/com/intellij/util/diff/Reindexer.kt

**BitSet type:** `com.intellij.util.diff.BitSet` (кастомный)
**Import:** Нет явного импорта — тот же пакет.
**Методы:** `BitSet(Int)` (конструктор), `.get(Int)` (operator), `.set(Int, Boolean)` (operator), `.set(Int, Int)` (range set)
**Контекст:** Переиндексация результатов diff после отбрасывания уникальных элементов. Принимает `Array<BitSet>` от LCS-алгоритмов, создаёт новые BitSet-ы полного размера, транслирует индексы обратно. Интенсивно использует побитовый доступ и диапазонную установку.

---

### 3. Platform: Util — Indexing Containers (BitSetAsRAIntContainer, IdBitSet)

#### platform/util/src/com/intellij/util/indexing/containers/BitSetAsRAIntContainer.java

**Тип:** Обёртка над `java.util.BitSet`
**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet;`
**Определяемые методы:** `BitSetAsRAIntContainer()`, `BitSetAsRAIntContainer(int)`, `add(int)`, `remove(int)`, `intIterator()`, `compact()`, `size()`, `contains(int)`, `ensureContainerCapacity(int)`
**Внутреннее хранилище:** `java.util.BitSet myBitSet` + `AtomicInteger myElementsCount` для O(1) `size()`.
**Контекст:** Адаптер `java.util.BitSet` к интерфейсу `RandomAccessIntContainer`. Используется в системе индексации IntelliJ для хранения множеств ID файлов. Не потокобезопасен. Итератор использует `myBitSet.stream().iterator()`.

---

#### platform/util/src/com/intellij/util/indexing/containers/ChangeBufferingList.java

**BitSet type:** (косвенное использование через `IdBitSet`)
**Import:** Нет прямого импорта BitSet.
**Методы:** Создаёт `new IdBitSet(length)`, `new IdBitSet(changes, length, 0)`, `new IdBitSet(IdBitSet.calcMinMax(...), 0)`, `new SortedIdSet(changes, length)`, `new SortedIdSet(...)`. При росте `SortedIdSet` переходит к `IdBitSet` через `ensureContainerCapacity()`.
**Контекст:** Буферизирующий список изменений для системы индексации. Накапливает изменения последовательно, конвертирует в компактное хранилище (`IdBitSet` или `SortedIdSet`) при достижении порога MAX_FILES=20000 или при запросе состояния. `IdBitSet` выбирается для больших наборов.

---

#### platform/util/src/com/intellij/util/indexing/containers/IdBitSet.java

**Тип:** Кастомная реализация
**Определяемые методы:** `IdBitSet(int)`, `IdBitSet(int[], int, int)`, `IdBitSet(int[], int)`, `IdBitSet(RandomAccessIntContainer, int)`, `contains(int)`, `add(int)`, `remove(int)`, `size()`, `intIterator()`, `compact()`, `ensureContainerCapacity(int)`, `clone()`, `getMin()`, `getMax()`, `nextSetBit(int)` (private), `sizeInBytes(int, int)` (static), `calcMinMax(int[], int)` (static)
**Внутреннее хранилище:** `long[] bitSlots` (64 бита на элемент) + `int bitIndexBase` (смещение базы) + `int bitsSet` (счётчик) + `int maxNonZeroSlotIndex`.
**Контекст:** Специализированная BitSet-реализация для хранения множеств ID файлов. Отличается от `java.util.BitSet` тем, что хранит ID относительно `bitIndexBase = min(ids)`, что позволяет эффективно хранить большие ID с расходом памяти, пропорциональным **диапазону** ID, а не их абсолютному значению. Реализует `RandomAccessIntContainer`.

---

#### platform/util/src/com/intellij/util/indexing/containers/SortedIdSet.java

**BitSet type:** (косвенное использование)
**Import:** Нет прямого импорта BitSet.
**Методы:** `ensureContainerCapacity(int)` — при превышении `MAX_FILES` создаёт `new IdBitSet(this, count)` для апгрейда.
**Контекст:** Отсортированный набор ID на основе `int[]`. При превышении размера автоматически мигрирует данные в `IdBitSet`. Альтернативное представление для малых наборов ID.

---

### 4. Platform: Util — Text Matching

#### platform/util/text-matching/srcJvm/com/intellij/util/text/matching/BitSet.kt

**Тип:** Typealias (JVM-specific)
**Определение:** `internal typealias BitSet = java.util.BitSet`
**Контекст:** Абстракция над `java.util.BitSet` для мультиплатформенного модуля text-matching. Комментарий в коде: *«should be eliminated in favor of Kotlin BitSet when it becomes multiplatform: KT-55163»*. Прямая ссылка на задачу KT-55163. На JVM — просто typealias на `java.util.BitSet`.

---

#### platform/util/text-matching/src/com/intellij/psi/codeStyle/TypoTolerantMatcher.kt

**BitSet type:** `com.intellij.util.text.matching.BitSet` (typealias -> `java.util.BitSet` на JVM)
**Import:** `import com.intellij.util.text.matching.BitSet`
**Методы:** `BitSet()` (конструктор), `.set(Int)`, `.get(Int)` (через `ErrorState.isAffected()`)
**Контекст:** Алгоритм нечёткого (typo-tolerant) сопоставления паттернов. Используется `BitSet` внутри `ErrorState` для отслеживания индексов, затронутых ошибками (опечатки, пропуски, перестановки символов). Используется через мультиплатформенный typealias.

---

### 5. Platform: Util — UI

#### platform/util/ui/src/com/intellij/util/ui/ExtendableHTMLViewFactory.kt

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet`
**Методы:** `BitSet()` (конструктор) — 2 вызова внутри `DetailsSummarySupportExtension.init`.
**Контекст:** Фабрика HTML-представлений (View) для расширяемого HTML-рендеринга в IDE. Используется `BitSet()` при регистрации кастомных HTML-тегов (`<details>`, `<summary>`) через `javax.swing.text.html.parser.DTD.defineElement(...)`, который требует `BitSet` для параметров `inclusions` и `exclusions`. Технически — использование Java Swing API, не собственная логика.

---

#### platform/util/ui/src/com/intellij/util/ui/html/utils.kt

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet`
**Методы:** Используется как тип поля `spaceMap: BitSet` в data-классе `JustificationInfo`. Получается через рефлексию из `GlyphView.JustificationInfo.spaceMap`.
**Контекст:** Утилиты для HTML-рендеринга. `BitSet` хранит карту пробелов для выравнивания текста. Данные извлекаются рефлексией из приватного внутреннего класса Swing `GlyphView.JustificationInfo`. Использование диктуется API javax.swing.

---

### 6. Platform: Util — Тесты

#### platform/util/testSrc/com/intellij/util/diff/IntLCSAutoTest.java (тест)

**BitSet type:** `com.intellij.util.diff.BitSet` (кастомный)
**Import:** Нет явного — тот же пакет.
**Методы:** `BitSet(Int)` (конструктор), `.nextClearBit(Int)`, `.get(Int)`, `verifyLCS()`
**Контекст:** Автоматические тесты MyersLCS и PatienceIntLCS на случайных данных (1000 итераций, длина до 300, 20 символов). Создаёт `BitSet` для хранения изменений, передаёт в LCS-алгоритмы, затем проверяет корректность через `verifyLCS()` (навигация по clear bits).

---

#### platform/util/testSrc/com/intellij/util/diff/IntLCSNewTest.java (тест)

**BitSet type:** `com.intellij.util.diff.BitSet` (кастомный)
**Import:** Нет явного — тот же пакет.
**Методы:** `MyersLCS.getChanges()` -> `BitSet[]`, `.get(Int)`
**Контекст:** Юнит-тесты MyersLCS. Проверяет конкретные сценарии: равные последовательности, различия в начале/конце, полностью различные, перемещение уникальных элементов, перемещение функций.

---

#### platform/util/testSrc/com/intellij/util/diff/IntLCSTest.java (тест)

**BitSet type:** `com.intellij.util.diff.BitSet` (кастомный)
**Import:** Нет явного — тот же пакет.
**Методы:** `MyersLCS.getChanges()` -> `BitSet[]`, `.cardinality()`
**Контекст:** Юнит-тесты для LCS через `Reindexer`. Тестирует `Diff.Change` после реиндексации. Метод `countChanges()` использует `cardinality()` для подсчёта суммарного числа изменений.

---

#### platform/util/testSrc/com/intellij/util/diff/PatienceIntLCSTest.java (тест)

**BitSet type:** `com.intellij.util.diff.BitSet` (кастомный)
**Import:** Нет явного — тот же пакет.
**Методы:** `PatienceIntLCS.getChanges()` -> `BitSet[]`, `.get(Int)`
**Контекст:** Юнит-тесты PatienceIntLCS. Те же сценарии, что в IntLCSNewTest, плюс: дополнительные edge-case (bug1, innerChunks 1-4), тесты на прерывание (failOnSmallReduction).

---

#### platform/util/testSrc/com/intellij/util/diff/ReindexerNewTest.java (тест)

**BitSet type:** `com.intellij.util.diff.BitSet` (кастомный)
**Import:** Нет явного — тот же пакет.
**Методы:** `BitSet()` (конструктор), `.set(Int, Int, Boolean)` (range set)
**Контекст:** Юнит-тесты Reindexer. Создаёт `BitSet[]` внутри LCSBuilder, помечает изменённые диапазоны, затем верифицирует через `IntLCSAutoTest.verifyLCS()`. Тестирует уникальные элементы, перемещения функций, внутренние чанки, случайные данные.

---

#### platform/util/testSrc/com/intellij/util/indexing/containers/BitSetAsRAIntContainerTest.java (тест)

**BitSet type:** (косвенно `java.util.BitSet` через `BitSetAsRAIntContainer`)
**Контекст:** Тест `BitSetAsRAIntContainer`. Наследует `RandomAccessIntContainerGenericTest`, создаёт экземпляр `new BitSetAsRAIntContainer()`. Проверяет generic-контракт RandomAccessIntContainer.

---

#### platform/util/testSrc/com/intellij/util/indexing/containers/IdBitSetTest.java (тест)

**BitSet type:** (косвенно `long[]` через `IdBitSet`)
**Контекст:** Тест `IdBitSet`. Наследует `RandomAccessIntContainerGenericTest`, создаёт экземпляр `new IdBitSet(16)`. Проверяет generic-контракт RandomAccessIntContainer.

---

#### platform/util/testSrc/com/intellij/util/indexing/containers/InputIdContainerTest.java (тест)

**BitSet type:** (косвенно через `IdBitSet`, `SortedIdSet`)
**Контекст:** Тест пустых ID-контейнеров. Проверяет `IdBitSet(0)`, `IdBitSet(123)`, `SortedIdSet(0)`, `SortedIdSet(123)` — что size=0 и итератор пуст.

---

#### platform/util/testSrc/com/intellij/util/indexing/containers/UpgradableRandomAccessIntContainerTests.java (тест)

**BitSet type:** (косвенно через `BitSetAsRAIntContainer`)
**Контекст:** Тест `UpgradableRandomAccessIntContainer` с upgrade-стратегией `IntHashSetAsRAIntContainer` -> `BitSetAsRAIntContainer`. Проверяет корректность работы при автоматической миграции между представлениями.

---

### 7. Platform: Analysis-Impl (Dataflow)

#### platform/analysis-impl/src/com/intellij/codeInspection/dataFlow/interpreter/ReachabilityCountingInterpreter.java

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet;`
**Методы:** `new BitSet()`, `.set(int)`, `.set(int, int)` (range), передача как параметр в `computeUnreachable(BitSet)`
**Контекст:** Отслеживание достижимости инструкций в интерпретаторе dataflow-анализа. Поле `myReached` хранит множество индексов достигнутых инструкций; при каждой обработке инструкции её индекс помечается в BitSet.

#### platform/analysis-impl/src/com/intellij/codeInspection/dataFlow/lang/ir/ControlFlow.java

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet;`
**Методы:** `.nextSetBit(int)` (в `computeUnreachable`)
**Контекст:** Метод `computeUnreachable(BitSet reached)` принимает BitSet достигнутых инструкций и определяет, какие PSI-элементы не были достигнуты (нет ни одной инструкции между start и end offset). Используется совместно с `ReachabilityCountingInterpreter`.

#### platform/analysis-impl/src/com/intellij/codeInspection/dataFlow/lang/ir/LiveVariablesAnalyzer.java

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet;`
**Методы:** `new BitSet()`, `.clone()`, `.andNot(BitSet)`, `.or(BitSet)`, `.stream()`, `.get(int)`, `.set(int)`, `.isEmpty()`, `.equals()`, `.hashCode()`, `.nextSetBit(int)`
**Контекст:** Анализ живых переменных для dataflow. Внутренний класс `ProcessedState` хранит `BitSet processedVars` для отслеживания обработанных дескрипторов переменных. В методе `flushDeadVariablesOnStatementFinish` создаётся `Map<FinishElementInstruction, BitSet> toFlush` для определения переменных, которые нужно сбросить в конце блока.

#### platform/analysis-impl/src/com/intellij/codeInspection/dataFlow/memory/DfaMemoryStateImpl.java

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet;`
**Методы:** `new BitSet()`, `.get(int)`, `.set(int)`, `.clear(int)`, `.stream()`, `.anyMatch()` (через `stream()`), `.nextSetBit(int)`, `Int2ObjectMap<BitSet>` (граф)
**Контекст:** Состояние памяти dataflow-анализатора. BitSet используется в нескольких контекстах: (1) `Int2ObjectMap<BitSet> graph` для проверки циклов в distinct-парах классов эквивалентности, (2) `visited`/`stack` BitSet для обхода графа (DFS), где `stack.clear(v)` сбрасывает бит после обработки вершины, (3) `flushVariables` создаёт BitSet для сбора всех переменных из классов эквивалентности.

#### platform/analysis-impl/src/com/intellij/codeInspection/dataFlow/memory/SortedIntSet.java

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet;`
**Методы:** `.set(int)` (в методе `setAll(BitSet)`)
**Контекст:** Вспомогательная структура данных для хранения отсортированного множества int. Метод `setAll(BitSet bitSet)` копирует все значения из SortedIntSet в переданный BitSet. Используется в DfaMemoryStateImpl для построения множества переменных.

#### platform/analysis-impl/src/com/intellij/codeInspection/dataFlow/rangeSet/LongRangeSet.java

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet;`
**Методы:** `BitSet.valueOf(long[])` (статический фабричный метод)
**Контекст:** Представление множеств диапазонов long-значений. BitSet используется только для визуализации: `BitSet.valueOf(new long[]{myBits})` в методе `toString()` для отображения остатков по модулю в человекочитаемом формате.

#### platform/analysis-impl/src/com/intellij/codeInsight/template/impl/TemplateState.java

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet;`
**Методы:** `new BitSet()`, `.clear()`, `.set(int)`, `.get(int)`, `.isEmpty()`
**Контекст:** Управление состоянием live-шаблонов в редакторе. Локальная переменная `calcedSegments` типа BitSet отслеживает, какие сегменты шаблона были пересчитаны (значение изменилось), чтобы не обновлять документ для неизменённых сегментов. `isEmpty()` используется в условии цикла повторного пересчёта.

---

### 8. Platform: Core-Impl

#### platform/core-impl/src/com/intellij/lang/impl/MarkerOptionalData.java

**BitSet type:** `java.util.BitSet` (наследование: `class MarkerOptionalData extends BitSet`)
**Import:** `import java.util.BitSet;`
**Методы:** `.get(int)`, `.set(int)`, `.set(int, boolean)` — унаследованные методы BitSet
**Контекст:** Класс *наследует* `java.util.BitSet` и использует его как флаги наличия опциональных данных для маркеров PSI-парсера (PsiBuilder). Метод `markAsHavingOptionalData` вызывает `set(markerId)`, метод `clean` проверяет `get(markerId)` и вызывает `set(markerId, false)`. BitSet здесь служит компактным индикатором наличия optional данных.

#### platform/core-impl/src/com/intellij/openapi/editor/impl/PsiBasedStripTrailingSpacesFilter.java

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet;`
**Методы:** `new BitSet(int)`, `.get(int)`, `.set(int, int)` (range set)
**Контекст:** Фильтр удаления trailing spaces в редакторе. Поле `myDisabledLinesBitSet` хранит номера строк, на которых удаление пробелов запрещено. Инициализируется с размером `document.getLineCount()`. Метод `disableRange` устанавливает диапазон строк; `isStripSpacesAllowedForLine` проверяет конкретную строку.

#### platform/core-impl/src/com/intellij/openapi/vfs/CompactVirtualFileSet.java

**BitSet type:** `java.util.BitSet` (внутри `PartitionedBitSetStorage` и `BitSetIterator`)
**Import:** `import java.util.BitSet;`
**Методы:** `new BitSet(int)`, `.get(int)`, `.set(int)`, `.clear(int)`, `.cardinality()`, `.isEmpty()`, `.nextSetBit(int)`, `computeIfAbsent`
**Контекст:** Компактное множество VirtualFile. BitSet используется в двух внутренних storage-реализациях: (1) `PartitionedBitSetStorage` — хранит `Int2ObjectMap<BitSet>` с партициями размером 2048 бит, (2) `BitSetIterator` — итератор по BitSet, использующий `nextSetBit`. Это часть адаптивной системы хранения fileId, которая переключается между IntSet, IdBitSet и PartitionedBitSet в зависимости от размера.

#### platform/core-impl/src/com/intellij/openapi/vfs/DeduplicatingVirtualFileFilter.kt

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet`
**Методы:** `BitSet()`, `.get(int)`, `.set(int)`
**Контекст:** Фильтр для дедупликации виртуальных файлов. Поле `visited` хранит множество fileId уже обработанных файлов. При вызове `accept` проверяется, посещён ли файл (`get`), и помечается как посещённый (`set`). Возвращает `true`, если файл ещё не был посещён.

---

### 9. Platform: Indexing

#### platform/indexing-api/src/com/intellij/util/indexing/IdFilter.java

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet;`
**Методы:** `new BitSet()`, `.set(int)`, `.get(int)`, `.cardinality()`
**Контекст:** Фильтр идентификаторов файлов проекта для индексации. В `buildProjectIdFilterForContentFiles` создаётся BitSet, куда записываются fileId всех файлов контента проекта. Возвращаемый `IdFilter` использует `idSet.get(id)` для проверки принадлежности файла проекту.

#### platform/indexing-api/src/com/intellij/util/indexing/roots/IndexableFilesDeduplicateFilter.java

**BitSet type:** `com.intellij.util.containers.ConcurrentBitSet`
**Import:** `import com.intellij.util.containers.ConcurrentBitSet;`
**Методы:** `ConcurrentBitSet.create()`, `.set(int)` (возвращает boolean — был ли бит уже установлен)
**Контекст:** Потокобезопасный фильтр дедупликации файлов при параллельной итерации индексируемых файлов. Поле `myVisitedFileSet` хранит fileId уже посещённых файлов. Метод `.set(fileId)` возвращает предыдущее значение бита, что позволяет атомарно определить, был ли файл уже обработан другим потоком.

#### platform/indexing-impl/src/com/intellij/psi/stubs/LazyStubList.java

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet;`
**Методы:** `new BitSet()`, `.set(int)`, `.nextSetBit(int)`
**Контекст:** Ленивая десериализация stub-деревьев. Внутренний класс `LazyStubData` содержит поле `myAllStarts` типа BitSet, хранящее позиции начала сериализованных данных каждого stub. Метод `stubBytes(int index)` использует `nextSetBit(start + 1)` для определения конца блока данных stub.

#### platform/indexing-impl/src/com/intellij/psi/stubs/StubTreeSerializerBase.java

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet;`
**Методы:** `new BitSet()`, `.set(int)`
**Контекст:** Сериализация/десериализация stub-деревьев. В `deserializeStubList` создаётся `BitSet allStarts`, куда при десериализации каждого stub записывается его начальная позиция через `allStarts.set(start)`. Этот BitSet затем передаётся в `LazyStubData` для дальнейшего использования.

---

### 10. Platform: Lang-Api и Lang-Impl

#### platform/lang-api/src/com/intellij/util/indexing/LightDirectoryIndex.java

**BitSet type:** `com.intellij.util.containers.ConcurrentBitSet`
**Import:** `import com.intellij.util.containers.ConcurrentBitSet;`
**Методы:** `ConcurrentBitSet.create()`, `.get(int)`, `.set(int)`, `.clear()`
**Контекст:** Лёгкая реализация индекса директорий. Поле `myNonInterestingIds` хранит fileId директорий, для которых не найдена полезная информация. Используется как кэш отрицательных результатов: при обходе иерархии директорий вверх, если id уже в ConcurrentBitSet — обход прерывается.

#### platform/lang-impl/src/com/intellij/codeInsight/editorActions/wordSelection/InjectedFileReferenceSelectioner.java

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet;`
**Методы:** `new BitSet(int)`, `new BitSet(0)`, `.get(int)`, `.set(int)`, `StreamEx...toBitSet()`
**Контекст:** Выделение слов/ссылок внутри инжектированных файлов в строковых литералах. Два BitSet: (1) `charEscapeLocations` — позиции escape-символов `\` в строковом литерале, (2) `compositeIndexes` — позиции символов, принадлежащих составным (нелистовым) PSI-элементам. Оба используются для корректного определения границ сегментов при Ctrl+W.

#### platform/lang-impl/src/com/intellij/find/impl/FindInProjectTask.java

**BitSet type:** `com.intellij.util.containers.ConcurrentBitSet`
**Import:** `import com.intellij.util.containers.ConcurrentBitSet;`
**Методы:** `ConcurrentBitSet.create()`, `.set(int)` (возвращает boolean)
**Контекст:** Поиск по проекту (Find in Files). Локальная переменная `visitedFileIds` в `unfoldAndProcessSearchItems` используется для дедупликации файлов при многопоточном обходе. `visitedFileIds.set(fileWithId.getId())` возвращает true, если файл уже был посещён, и в этом случае он пропускается.

#### platform/lang-impl/src/com/intellij/openapi/roots/impl/FilesScanExecutor.kt

**BitSet type:** `com.intellij.util.containers.ConcurrentBitSet`
**Import:** `import com.intellij.util.containers.ConcurrentBitSet`
**Методы:** `ConcurrentBitSet.create(int)`, `.set(int)` (возвращает boolean)
**Контекст:** Параллельный обход файлов в scope при поиске. В `processFilesInScope` создаётся `visitedFiles = ConcurrentBitSet.create(deque.size)` для дедупликации файлов. `visitedFiles.set(fileId)` возвращает true, если файл уже был обработан — тогда он пропускается.

#### platform/lang-impl/src/com/intellij/psi/search/MappedFileTypeIndex.java

**BitSet type:** Косвенное использование через `BitSetAsRAIntContainer` (обёртка над `java.util.BitSet`)
**Import:** `import com.intellij.util.indexing.containers.BitSetAsRAIntContainer;`
**Методы:** `new BitSetAsRAIntContainer(int)` (при конвертации из IntHashSet)
**Контекст:** Инвертированный индекс типов файлов. При превышении порога `INVERTED_INDEX_SIZE_THRESHOLD` (16384) контейнер `IntHashSetAsRAIntContainer` апгрейдится до `BitSetAsRAIntContainer` через `UpgradableRandomAccessIntContainer`. BitSet здесь используется для компактного хранения больших множеств fileId.

#### platform/lang-impl/src/com/intellij/util/indexing/impl/storage/IntLog.kt

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet`
**Методы:** `BitSet()`, `.get(int)`, `.set(int)`
**Контекст:** Лог данных индексации. В `processEntries` создаётся `uniqueInputs = BitSet()` для подсчёта уникальных и повторных записей в логе. Если `uniqueInputs.get(inputId)` возвращает true, запись считается бесполезной (дубликат). Соотношение useless/useful определяет необходимость компакции.

#### platform/lang-impl/src/com/intellij/util/indexing/projectFilter/CachingProjectIndexableFilesFilter.kt

**BitSet type:** `com.intellij.util.containers.ConcurrentThreeStateBitSet`
**Import:** `import com.intellij.util.containers.ConcurrentThreeStateBitSet`
**Методы:** `ConcurrentThreeStateBitSet.create()`, `[fileId]` (get, возвращает Boolean?), `[fileId] = value` (set), `.compareAndSet(int, Boolean?, Boolean)`, `.clear()`, `.size()`
**Контекст:** Кэширующий фильтр индексируемых файлов проекта. Использует *трёхзначную* битовую карту (true/false/null) для хранения статуса каждого fileId. Null означает «неизвестно, нужно вычислить». Поддерживает CAS-операцию для потокобезопасной ленивой инициализации.

#### platform/lang-impl/src/com/intellij/util/indexing/projectFilter/ConcurrentFileIds.kt

**BitSet type:** `com.intellij.util.containers.ConcurrentBitSet`
**Import:** `import com.intellij.util.containers.ConcurrentBitSet`
**Методы:** `ConcurrentBitSet.create()`, `.get(int)`, `.set(int, Boolean)`, `.clear()`, `.cardinality()`, `.size()`, `.toIntArray()`, `ConcurrentBitSet.readFrom(DataInputStream)`
**Контекст:** Обёртка над ConcurrentBitSet для хранения множества fileId с поддержкой сериализации. Предоставляет `writeTo(DataOutputStream)` и `readFrom(DataInputStream)` для персистентного хранения битовой маски файлов.

#### platform/lang-impl/src/com/intellij/util/indexing/projectFilter/ProjectIndexableFilesFilterHealthCheck.kt

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet`
**Методы:** `BitSet()`, `.set(int)`, `[int]` (get), `.or(BitSet)`, `.size`, `.get(int)`
**Контекст:** Проверка здоровья (healthcheck) фильтра индексируемых файлов. Класс `IndexableFiles` хранит `allFiles: BitSet` (все файлы, которые должны быть индексируемы) и `perProvider: List<Pair<IndexableFilesIterator, BitSet>>` (файлы по каждому провайдеру). `IndexableFilesSetWithProvidersHandler` создаёт отдельный BitSet на провайдера, затем объединяет через `.or()`. В `doRunHealthCheck` создаётся `filesInFilter: BitSet` для сравнения с фильтром.

#### platform/lang-impl/src/com/intellij/util/indexing/contentQueue/dev/IndexWriter.kt

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet`
**Методы:** `BitSet(int)`, `.set(int)`, `.cardinality()`
**Контекст:** Запись результатов индексации. В нескольких реализациях `writeChangesToIndexes` создаётся `workersToSchedule = BitSet(workersCount)` для определения, какие воркеры нужно активировать для обработки конкретного результата индексации файла. Затем `.cardinality()` используется для расчёта `updatesLeftCounter`.

#### platform/lang-impl/src/com/intellij/util/indexing/events/DirtyFiles.kt

**BitSet type:** `com.intellij.util.containers.ConcurrentBitSet`
**Import:** `import com.intellij.util.containers.ConcurrentBitSet`
**Методы:** `ConcurrentBitSet.create()`, `.set(int)` (возвращает boolean), `.get(int)`, `.clear(int)` (возвращает boolean), `.clear()`, `.size()`, `[int]` (get)
**Контекст:** Отслеживание «грязных» (изменённых) файлов по проектам. Класс `ProjectDirtyFiles` содержит `filesSet: ConcurrentBitSet` для потокобезопасного хранения fileId. Метод `addAllTo(IntSet)` итерирует все биты для экспорта в IntSet.

---

### 11. Platform: Syntax (MutableBitSet)

#### platform/syntax/syntax-api/src/com/intellij/platform/syntax/impl/util/MutableBitSet.kt

**BitSet type:** Кастомная реализация `MutableBitSet` (не `java.util.BitSet`)
**Import:** Нет внешних зависимостей на BitSet
**Хранилище:** `private var bitset = LongArray(16) { 0 }` — массив long-слов
**Полное API MutableBitSet:**
- `fun add(markerId: Int)` — устанавливает бит (ensureCapacity + побитовое OR)
- `fun contains(markerId: Int): Boolean` — проверяет бит (побитовое AND)
- `fun remove(markerId: Int)` — сбрасывает бит (побитовое AND с инвертированной маской)
- `private fun ensureCapacity(markerId: Int)` — увеличивает массив в 1.5 раза при необходимости

**Также в этом же файле — класс `BitSet` (immutable, `internal class BitSet(ints: IntList)`):**
**Хранилище:** `private val bitset: LongArray`, `private val shift: Int`, `private val max: Int`
**API:**
- Конструктор принимает `IntList`, вычисляет min/max, создаёт оптимизированный LongArray со сдвигом (`shift = min shr 6`)
- `fun contains(i: Int): Boolean` — проверяет бит с учётом сдвига
- `fun isEmpty(): Boolean` — проверяет размер массива

**Контекст:** Собственная реализация bitset для нового Syntax API платформы. `MutableBitSet` — мутабельный вариант с динамическим расширением, `BitSet` — иммутабельный вариант с оптимизацией по сдвигу (не аллоцирует пустые слова перед минимальным элементом). Используется в парсере вместо `java.util.BitSet` для минимизации зависимостей.

#### platform/syntax/syntax-api/test/com/intellij/platform/syntax/impl/builder/BitSetTest.kt (тест)

**BitSet type:** `com.intellij.platform.syntax.impl.util.MutableBitSet`
**Import:** `import com.intellij.platform.syntax.impl.util.MutableBitSet`
**Методы:** `MutableBitSet()`, `.add(Int)`, `.contains(Int)`, `.remove(Int)`
**Контекст:** (тест) Тесты для кастомной реализации `MutableBitSet`. Три теста: пустой набор, add/remove для индексов 0..1000, массовый add 0..1000 с проверкой contains и remove.

#### platform/syntax/syntax-api/src/com/intellij/platform/syntax/SyntaxElementTypeSet.kt

**BitSet type:** `com.intellij.platform.syntax.impl.util.BitSet` (кастомная immutable-реализация)
**Import:** `import com.intellij.platform.syntax.impl.util.BitSet`
**Методы:** `BitSet(IntArrayList)`, `.contains(int)`, `.isEmpty()`
**Контекст:** Множество типов синтаксических элементов. `SyntaxElementTypeSet` оборачивает кастомный `BitSet` для быстрой проверки принадлежности элемента по его индексу. Поддерживает операции `+`, `-`, `intersect`. BitSet хранит индексы `SyntaxElementType.index`.

#### platform/syntax/syntax-api/src/com/intellij/platform/syntax/impl/builder/MarkerOptionalData.kt

**BitSet type:** `com.intellij.platform.syntax.impl.util.MutableBitSet`
**Import:** `import com.intellij.platform.syntax.impl.util.MutableBitSet`
**Методы:** `MutableBitSet()`, `.contains(int)`, `.add(int)`, `.remove(int)`
**Контекст:** Kotlin-аналог `com.intellij.lang.impl.MarkerOptionalData` из core-impl, но использует кастомный `MutableBitSet` вместо наследования от `java.util.BitSet`. Хранит флаги наличия опциональных данных (binders, errors, collapsed state) для маркеров парсера нового Syntax API.

---

### 12. Platform: Platform-Api и Platform-Impl

**platform/platform-api** (1 файл)

#### platform/platform-api/src/com/intellij/ui/OptionGroup.java

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet;`
**Методы:** `new BitSet()`, `.set(int, boolean)`, `.get(int)`
**Контекст:** Устаревший (deprecated) UI-компонент для панелей опций. Поле `myIndented` хранит флаги, какие элементы в панели должны быть с отступом. При добавлении компонента через `add(..., boolean indented)` устанавливается бит; при отрисовке `get(i)` определяет отступ.

**platform/platform-impl** (5 файлов)

#### platform/platform-impl/initial-config-import/src/com/intellij/ide/OldDirectoryCleaner.java

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet;`
**Методы:** `new BitSet()`, `.set(int, boolean)`, `.get(int)`, `.cardinality()`, `::get` (method reference)
**Контекст:** Очистка устаревших директорий IDE. Внутренний класс `MenuTableModel` использует `mySelected = new BitSet()` для хранения выбранных пользователем строк в таблице диалога. `.cardinality()` используется для обновления текста кнопки "Delete (N)". `.get(row)` и `.set(row, value)` — для чтения/записи состояния чекбоксов.

#### platform/platform-impl/src/com/intellij/openapi/actionSystem/impl/PreCachedDataContext.kt

**BitSet type:** `com.intellij.util.containers.ConcurrentBitSet`
**Import:** `import com.intellij.util.containers.ConcurrentBitSet`
**Методы:** `ConcurrentBitSet.create()`, `.get(int)`, `.set(int)`
**Контекст:** Кэш данных контекста для action system. Поле `nullsByRules` внутреннего класса `CachedData` хранит индексы правил, для которых результат был null. Используется для избежания повторного вычисления null-результатов в `DataManager.getDataFromRules`.

#### platform/platform-impl/src/com/intellij/openapi/fileTypes/impl/IgnoredFileCache.java

**BitSet type:** `com.intellij.util.containers.ConcurrentBitSet`
**Import:** `import com.intellij.util.containers.ConcurrentBitSet;`
**Методы:** `ConcurrentBitSet.create()`, `.get(int)`, `.set(int)`, `.clear(int)`, `.clear()`
**Контекст:** Кэш для проверки, является ли файл «игнорируемым» по имени. `myNonIgnoredIds` хранит fileId файлов, которые точно *не* игнорируемы (кэш отрицательных результатов). При переименовании файла бит сбрасывается; при полном изменении паттернов кэш очищается.

#### platform/platform-impl/src/com/intellij/openapi/vfs/newvfs/impl/VfsData.java

**BitSet type:** `com.intellij.util.containers.ConcurrentBitSet`
**Import:** `import com.intellij.util.containers.ConcurrentBitSet;`
**Методы:** `ConcurrentBitSet.create()`, `.get(int)`, `.set(int)`
**Контекст:** Основной in-memory кэш VFS. Поле `invalidatedFileIds` типа ConcurrentBitSet хранит fileId инвалидированных (удалённых) файлов. Создаётся через `create()`, записывается через `set(id)` в `invalidateFile()`, читается через `get(id)` в `isFileValid()`. Используется как потокобезопасный negative-cache с чтением и записью.

#### platform/platform-impl/src/com/intellij/openapi/vfs/newvfs/persistent/CompactRecordsTable.java

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet;`
**Методы:** `new BitSet()`, `.set(int)`, `.get(int)`
**Контекст:** Компактная таблица записей для VFS-хранилища. Метод `buildIdSetOfExtraRecords()` создаёт BitSet и записывает в него id «extra»-записей (indirect records для overflow address/size/capacity). `createRecordIdIterator()` использует этот BitSet для пропуска extra-записей при итерации.

---

### 13. Platform: Прочее (projectModel-impl, todo)

**platform/projectModel-impl** (1 файл)

#### platform/projectModel-impl/src/com/intellij/workspaceModel/core/fileIndex/impl/WorkspaceFileIndexDataImpl.kt

**BitSet type:** `com.intellij.util.containers.ConcurrentBitSet`
**Import:** `import com.intellij.util.containers.ConcurrentBitSet`
**Методы:** `ConcurrentBitSet.create()`, `.get(int)`, `.set(int)`, `.clear()`
**Контекст:** Реализация индекса файлов рабочего пространства. Поле `fileIdWithoutFileSets` хранит fileId, для которых не найдено ни одного FileSet (кэш отрицательных результатов). Создаётся через `create()`, читается через `.get(fileId)`, помечается через `.set(fileId)`, сбрасывается через `.clear()` в `resetFileCache()`. Используется как потокобезопасный negative-cache с чтением, записью и полным сбросом.

**platform/todo** (1 файл)

#### platform/todo/src/com/intellij/psi/impl/cache/impl/IndexTodoCacheManagerImpl.java

**BitSet type:** `com.intellij.util.containers.ConcurrentBitSet`
**Import:** `import com.intellij.util.containers.ConcurrentBitSet;`
**Методы:** `ConcurrentBitSet.create()`, `.set(int)`, `.clear(int)`, `.nextSetBit(int)`
**Контекст:** Кэш-менеджер TODO-комментариев. В `processFilesWithTodoItems` создаётся `idSet = ConcurrentBitSet.create()` для сбора fileId файлов с TODO. Сначала собираются id из индекса (`set`), затем для unsaved-документов id очищается (`clear`), затем для устаревших записей id очищается. Финальный обход через `nextSetBit`.

---

### 14. Platform: Тесты (platform-tests)

#### platform/platform-tests/testSrc/com/intellij/openapi/vfs/newvfs/persistent/VfsDiffBuilder.kt (тест)

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet`
**Методы:** `BitSet()`, `.set(int)`, `[int]` / `.get(int)`
**Контекст:** (тест) Утилита для построения diff между двумя VFS-снимками. Локальная переменная `visitedIds = BitSet()` отслеживает уже посещённые fileId при BFS-обходе дерева файлов (`set`), а затем используется как membership-фильтр при обходе attribute storage для отбора атрибутов по уже посещённым `fileId` (`visitedIds[fileId]`).

#### platform/platform-tests/testSrc/com/intellij/util/containers/ConcurrentBitSetTest.java (тест)

**BitSet type:** `com.intellij.util.containers.ConcurrentBitSet`
**Import:** `import com.intellij.util.containers.ConcurrentBitSet;`
**Методы:** `ConcurrentBitSet.create()`, `.set(int)`, `.set(int, boolean)`, `.get(int)`, `.clear(int)`, `.clear()`, `.nextSetBit(int)`, `.nextClearBit(int)`, `.cardinality()`
**Контекст:** (тест) Тесты для `ConcurrentBitSet`: (1) `testSanity` — корректность set/clear/get/nextSetBit/nextClearBit для 3000 индексов, (2) `testStressFineGrainedSmallSetModifications` и `testStressCoarseGrainedBigSet` — параллельные stress-тесты с 4 потоками, (3) `testParallelReadPerformance` — бенчмарк параллельного чтения, (4) `testSetPerformance` — бенчмарк последовательного set/get.

### 15. Platform: Diff-Impl

#### platform/diff-impl/src/com/intellij/diff/comparison/ComparisonManagerImpl.java

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet`
**Методы:** `new BitSet()`, `.set(start, end)`, `.nextClearBit(start)`, `.get(index)`
**Контекст:** Основной менеджер сравнения текстов. BitSet используется для хранения «игнорируемых диапазонов» символов (форматирование, пробелы). Метод `collectIgnoredRanges()` создает BitSet из `List<TextRange>`. Далее этот BitSet пробрасывается в методы сравнения строк (`compareLinesWithIgnoredRanges`, `trimIgnoredInnerFragments` и т.д.), где через `nextClearBit`/`get` определяется, попадает ли символ/строка в игнорируемый диапазон.

#### platform/diff-impl/src/com/intellij/diff/merge/MergeThreesideViewerActions.kt

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet`
**Методы:** *получение BitSet из `DiffUtil.getSelectedLines(editor)`*, передача в `isChangeSelected(change, lines, side)`
**Контекст:** Действия для 3-стороннего merge-вьювера (apply, ignore, resolve selected changes). `BitSet` приходит из `DiffUtil.getSelectedLines()` и используется для определения, какие изменения выделены пользователем в редакторе. Сам файл BitSet не конструирует.

#### platform/diff-impl/src/com/intellij/diff/tools/combined/CombinedDiffViewer.kt

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet`
**Методы:** `BitSet(size)`, `.set(start, end, boolean)`, `.set(index, boolean)`, `.get(index)`
**Контекст:** Комбинированный diff-вьювер, объединяющий несколько diff-блоков. BitSet `collapsedDiffBlocks` хранит состояние «свернутых» блоков: `.set(0, size-1, true)` для массового сворачивания, `.set(index, collapseState)` для переключения отдельного блока, `.get(index)` для чтения текущего состояния. Каждый бит — один diff-блок, 1 = свернут.

#### platform/diff-impl/src/com/intellij/diff/tools/fragmented/UnifiedDiffViewer.java

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet`
**Методы:** *получение через `DiffUtil.getSelectedLines(myEditor)`*, передача в `DiffUtil.isSelectedByLine`
**Контекст:** Unified-diff вьювер. `getSelectedChanges()` получает BitSet выделенных строк и фильтрует список `UnifiedDiffChange`, проверяя пересечение диапазона каждого изменения с выделенными строками.

#### platform/diff-impl/src/com/intellij/diff/tools/simple/SimpleDiffViewer.java

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet`
**Методы:** *получение через `DiffUtil.getSelectedLines(editor)`*, передача в `isChangeSelected`
**Контекст:** Двусторонний diff-вьювер. Аналогичный паттерн: получение BitSet выделенных строк для определения, какие `SimpleDiffChange` попадают в выделение.

#### platform/diff-impl/src/com/intellij/diff/tools/simple/SimpleThreesideDiffViewer.java

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet`
**Методы:** *получение через `DiffUtil.getSelectedLines(editor)`*, передача в `isChangeSelected`
**Контекст:** Трехсторонний diff-вьювер. Тот же паттерн выделения строк для фильтрации `SimpleThreesideDiffChange`.

#### platform/diff-impl/src/com/intellij/diff/tools/util/text/SmartTextDiffProvider.java

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet`
**Методы:** *получение через `ComparisonManagerImpl.collectIgnoredRanges()`*
**Контекст:** Провайдер «умного» сравнения текстов. Собирает `ignoredRanges` от `DiffIgnoredRangeProvider` и превращает их в BitSet через `collectIgnoredRanges()`, затем передает в `ComparisonManagerImpl` для сравнения с учетом игнорируемых диапазонов.

#### platform/diff-impl/src/com/intellij/diff/util/DiffUtil.java

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet`
**Методы:** `new BitSet(totalLines + 1)`, `.set(line1, line2)`, `.get(line)`, `.nextClearBit(line1)`, `.nextSetBit(line1)`
**Контекст:** Центральная утилита diff-подсистемы. Метод `getSelectedLines(Editor)` создает BitSet, отмечая строки, выделенные каретками. `isSelectedByLine(BitSet, line1, line2)` проверяет, пересекается ли диапазон строк с BitSet. `isSomeRangeSelected` принимает `Condition<BitSet>`. Это **главный источник BitSet** для всех diff-вьюверов.

#### platform/diff-impl/src/com/intellij/openapi/vcs/ex/DocumentTracker.kt

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet`
**Методы:** `BitSet()` (конструктор), `.toByteArray()`, `BitSet.valueOf(ByteArray)`, `.set(start, end)`, `.nextSetBit(index)`, `.nextClearBit(index)`, `.or()`, `.length()`, `.isEmpty`, `.cardinality()`
**Контекст:** Трекер изменений документа. Вложенный sealed-класс `RangeExclusionState.Partial` хранит две BitSet (`includedDeletions`, `includedAdditions`) для частичного включения/исключения строк из коммита. Содержит `JavaBitSetSerializer` — кастомный KSerializer для сериализации `j.u.BitSet` через Base64 (для kotlinx.serialization). Методы `copyIncludedInto`, `iterateIncludedRanges`, `iterateOffsets` навигируют по BitSet для определения включенных диапазонов.

#### platform/diff-impl/src/com/intellij/openapi/vcs/ex/LineStatusTrackerBase.kt

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet`
**Методы:** *делегирование `getRangesForLines(lines: BitSet)` и `rollbackChanges(lines: BitSet)` в `blockOperations`*, `.isSelectedByLine(lines)` (extension)
**Контекст:** Базовый класс трекера изменений строк. Принимает BitSet выделенных строк и делегирует в `LineStatusTrackerBlockOperations`. Метод `rollbackChanges(BitSet)` откатывает изменения для выбранных строк.

#### platform/diff-impl/src/com/intellij/openapi/vcs/ex/LineStatusTrackerBlockOperations.kt

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet`
**Методы:** `DiffUtil.isSelectedByLine(lines, start, end)` (расширение), передача BitSet в фильтрацию блоков
**Контекст:** Операции над блоками трекера строк. `getRangesForLines(BitSet)` фильтрует блоки по пересечению с BitSet выделенных строк. Companion-расширение `BlockI.isSelectedByLine(BitSet)` проксирует в `DiffUtil`.

#### platform/diff-impl/src/com/intellij/openapi/vcs/ex/LineStatusTrackerI.kt

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet`
**Методы:** `getRangesForLines(lines: BitSet)`, `rollbackChanges(lines: BitSet)`
**Контекст:** Интерфейс трекера изменений строк. Определяет контракт: получение измененных диапазонов по BitSet строк и откат изменений по BitSet строк.

---

### 16. Platform: VCS-Impl

#### platform/vcs-impl/lang/src/com/intellij/codeInsight/actions/VcsFacadeImpl.java

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet`
**Методы:** `new BitSet()`, `.set(line1, line2)`, `.nextSetBit(startLine)`
**Контекст:** Фасад для интеграции VCS с code actions (форматирование, оптимизация импортов). Методы `createChangedLinesBitSet`, `createLinesBitSetBefore`, `createLinesBitSetAfter` конвертируют список `Range` в BitSet измененных строк. Затем `isElementChanged` проверяет, пересекается ли PSI-элемент с этим BitSet, для фильтрации элементов, затронутых изменениями VCS.

#### platform/vcs-impl/shared/src/com/intellij/openapi/vcs/changes/ui/ChangeListRemoteState.kt

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet`
**Методы:** `BitSet()`, `.set(index, value)`, `.isEmpty`
**Контекст:** Состояние удаленной синхронизации changelist-а. BitSet `notUpToDate` хранит индексы файлов, которые не актуальны. `report()` устанавливает бит, `allUpToDate()` проверяет пустоту.

#### platform/vcs-impl/src/com/intellij/openapi/vcs/changes/actions/diff/lst/LocalTrackerDiffUtil.kt

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet`
**Методы:** `BitSet()`, `.set(line)`, `.set(startLine, endLine)`, `.nextClearBit(start)`, `.nextSetBit(start)`, `.cardinality()`
**Контекст:** Утилиты diff для локального трекера changelist-ов. `toggleLinePartialExclusion` создает BitSet с одной строкой. `shouldShowToggleAreaThumb` создает две BitSet для проверки непрерывности выделения. `selectionCovers`/`selectionIntersects` используют `nextClearBit`/`nextSetBit` для проверки покрытия диапазона. `getLocalSelectedLines` собирает BitSet выделенных строк. Вложенный класс `SelectedTrackerLine` хранит `vcsLines: BitSet?` и `localLines: BitSet?`.

#### platform/vcs-impl/src/com/intellij/openapi/vcs/changes/actions/diff/lst/UnifiedLocalChangeListDiffViewer.kt

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet`
**Методы:** `BitSet()`, `.set(line)`, `.stream()` (через `DiffUtil.getSelectedLines(editor).stream().forEach`)
**Контекст:** Unified-вьювер для diff changelist-ов. `getSelectedTrackerLines` создает две BitSet (deletions, additions), маппит строки из unified-режима в оригинальные строки для трекера. `.stream()` используется для итерации по выделенным строкам.

#### platform/vcs-impl/src/com/intellij/openapi/vcs/changes/patch/AppliedTextPatch.java

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet`
**Методы:** `new BitSet()`, `.nextSetBit(start)`, `.set(start, end, true)`
**Контекст:** Представление примененного текстового патча. BitSet `appliedLines` используется для обнаружения перекрывающихся hunk-ов: при создании патча проверяется через `nextSetBit`, не пересекаются ли диапазоны уже примененных строк с новым hunk-ом.

#### platform/vcs-impl/src/com/intellij/openapi/vcs/changes/patch/tool/ApplyPatchViewer.java

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet`
**Методы:** *получение через `DiffUtil.getSelectedLines(editor)`*, передача в `isChangeSelected`
**Контекст:** Вьювер для применения патчей. Стандартный паттерн выделения строк — получение BitSet и фильтрация `ApplyPatchChange` по пересечению диапазонов.

#### platform/vcs-impl/src/com/intellij/openapi/vcs/ex/MoveChangesLineStatusAction.java

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet`
**Методы:** *получение через `DiffUtil.getSelectedLines(editor)`*, передача в `moveToAnotherChangelist(tracker, selectedLines)`
**Контекст:** Действие перемещения изменений между changelist-ами. Получает BitSet выделенных строк и передает в `PartialLocalLineStatusTracker.getRangesForLines(BitSet)` для определения затронутых диапазонов.

#### platform/vcs-impl/src/com/intellij/openapi/vcs/ex/RollbackLineStatusAction.java

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet`
**Методы:** *получение через `DiffUtil.getSelectedLines(editor)`*, передача в `tracker.rollbackChanges(selectedLines)`
**Контекст:** Действие отката изменений (revert). Получает BitSet выделенных строк и передает в `LineStatusTrackerI.rollbackChanges(BitSet)`.

#### platform/vcs-impl/src/com/intellij/openapi/vcs/ex/PartialLocalLineStatusTracker.kt

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet`
**Методы:** `BitSet()`, `.or()`, `.set(index, value)`, `.set(start, end, boolean)`, `.nextSetBit(index)`, `.nextClearBit(index)`, `.length()`, `DiffUtil.isSelectedByLine(lines, start, end)`
**Контекст:** Трекер для частичного управления changelist-ами. Ключевой файл: `moveToChangelist(BitSet, ...)`, `setExcludedFromCommit(BitSet, ...)`, `setPartiallyExcludedFromCommit(BitSet, Side, ...)`. Создает `includedAdditions`/`includedDeletions` BitSet для `RangeExclusionState.Partial`. Метод `iterateIncludedRangesBetween` навигирует по BitSet через `nextSetBit`/`nextClearBit`.

#### platform/vcs-impl/src/com/intellij/openapi/vcs/impl/ElementStatusTrackerImpl.java

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet`
**Методы:** `new BitSet()`, `.set(start, end + 1)`, *передача в `tracker.getRangesForLines(set)`*
**Контекст:** Трекер статуса элементов (файлов/фрагментов) в VCS. Создает BitSet из текстового диапазона элемента и запрашивает у `LineStatusTrackerI` измененные диапазоны для этого BitSet.

---

### 17. Platform: VCS-Log — Graph (UnsignedBitSet, BitSetFlags)

#### platform/vcs-log/graph/src/com/intellij/vcs/log/graph/utils/UnsignedBitSet.java

**BitSet type:** кастомная обертка; внутри — `java.util.BitSet`
**Import:** `import java.util.BitSet`
**Полный API:**
- `UnsignedBitSet()` — конструктор по умолчанию
- `UnsignedBitSet(BitSet positiveSet, BitSet negativeSet)` — конструктор с двумя j.u.BitSet
- `set(int bitIndex, boolean value)` — установка бита (поддержка отрицательных индексов)
- `set(int fromIndex, int toIndex, boolean value)` — range-set (boundaries включительно, поддержка отрицательных)
- `get(int bitIndex)` — чтение бита (поддержка отрицательных)
- `clone()` — глубокое копирование
**Контекст:** Расширение `j.u.BitSet` для поддержки **отрицательных индексов**. Хранит два `j.u.BitSet`: для положительных и отрицательных индексов. Используется в графе VCS-лога для хранения видимости узлов (nodeId может быть отрицательным). `@ApiStatus.Internal`.

#### platform/vcs-log/graph/src/com/intellij/vcs/log/graph/utils/impl/BitSetFlags.java

**BitSet type:** адаптер `Flags` над `java.util.BitSet`
**Import:** `import java.util.BitSet`
**Полный API (implements Flags):**
- `BitSetFlags(int size)` — конструктор, default=false
- `BitSetFlags(int size, boolean defaultValue)` — конструктор со значением по умолчанию
- `size(): int` — количество бит
- `get(int index): boolean` — чтение бита с проверкой границ
- `set(int index, boolean value)` — запись бита с проверкой границ
- `setAll(boolean value)` — установка всех бит через `myBitSet.set(0, mySize, value)`
- `equals(Object)`, `hashCode()`, `toString()`
**Контекст:** Адаптер `j.u.BitSet` к интерфейсу `Flags`. Добавляет фиксированный размер (`mySize`) и проверку границ. Используется повсеместно в graph-модуле как «visited»-множество для BFS/DFS.

#### platform/vcs-log/graph/src/com/intellij/vcs/log/graph/utils/BfsUtil.kt

**BitSet type:** `BitSetFlags` (через `Flags`)
**Import:** `import com.intellij.vcs.log.graph.utils.impl.BitSetFlags`
**Методы:** `BitSetFlags(graph.nodesCount())` — конструктор в secondary constructor `BfsWalk`, `.get(int)`, `.set(int, boolean)` — чтение/запись visited-флагов в `BfsWalk.step()`
**Контекст:** BFS-обход графа. `BitSetFlags` используется как дефолтный `visited` при конструировании `BfsWalk(start, graph)`. В центральном цикле `step()` флаги активно читаются (`visited.get(node)`) и записываются (`visited.set(node, true)`) для отслеживания посещённых узлов.

#### platform/vcs-log/graph/src/com/intellij/vcs/log/graph/utils/DfsUtil.kt

**BitSet type:** `BitSetFlags` (через `Flags`)
**Import:** `import com.intellij.vcs.log.graph.utils.impl.BitSetFlags`
**Методы:** `BitSetFlags(linearGraph.nodesCount())` — в secondary constructor `DfsWalk`, `.get(int)`, `.set(int, boolean)` — чтение/запись visited-флагов в `DfsWalk.walk()`
**Контекст:** DFS-обход графа. `BitSetFlags` используется как дефолтный `visited` для `DfsWalk`. В цикле `walk()` флаги активно читаются (`visited.get(start)`, `visited.get(downNode)`) и записываются (`visited.set(..., true)`) для отслеживания посещённых узлов.

#### platform/vcs-log/graph/src/com/intellij/vcs/log/graph/utils/GraphUtil.kt

**BitSet type:** `UnsignedBitSet`, `BitSetFlags`
**Import:** `import com.intellij.vcs.log.graph.utils.impl.BitSetFlags`
**Методы:**
- `UnsignedBitSet()`, `.set(from, to, true)`, `.get(nodeId)` — для reachable-nodes
- `BitSetFlags(nodesCount(), false)`, `.set(int, boolean)`, `.get(int)`, `.setAll(boolean)` — для visited в `isAncestor`, `subgraphDifference`, `getCorrespondingParent`
**Контекст:** Утилиты для работы с графом VCS-лога. `getReachableNodes` и `getReachableMatchingNodes` возвращают `UnsignedBitSet` видимых узлов. `isAncestor` и `subgraphDifference` используют `BitSetFlags` как visited-множества с активным чтением/записью (`get`/`set`). `getCorrespondingParent` переиспользует visited через `setAll(false)` между обходами.

#### platform/vcs-log/graph/src/com/intellij/vcs/log/graph/collapsing/CollapsedActionManager.java

**BitSet type:** `UnsignedBitSet`
**Import:** `import com.intellij.vcs.log.graph.utils.UnsignedBitSet`
**Методы:** `.get(nodeId)` — чтение через `matchedNodeId.get(linearGraph.getNodeId(nodeIndex))`
**Контекст:** Менеджер действий для свертки графа. Получает `UnsignedBitSet matchedNodeId` и проверяет, соответствует ли узел текущему фильтру.

#### platform/vcs-log/graph/src/com/intellij/vcs/log/graph/collapsing/CollapsedController.java

**BitSet type:** `UnsignedBitSet`
**Import:** `import com.intellij.vcs.log.graph.utils.UnsignedBitSet`
**Методы:** `GraphUtilKt.getReachableNodes(...)` — получение `UnsignedBitSet initVisibility`
**Контекст:** Контроллер свертки графа. Вычисляет начальную видимость узлов (`UnsignedBitSet`) через `getReachableNodes` и передает в `CollapsedGraph.newInstance`.

#### platform/vcs-log/graph/src/com/intellij/vcs/log/graph/collapsing/CollapsedGraph.java

**BitSet type:** `UnsignedBitSet`
**Import:** `import com.intellij.vcs.log.graph.utils.UnsignedBitSet`
**Методы:** `.clone()`, `.get(nodeId)`, `.set(nodeId, value)` (через `GraphNodesVisibility`)
**Контекст:** Ядро механизма свертки графа. Хранит `myMatchedNodeId` (UnsignedBitSet сопоставленных узлов) и делегирует видимость в `GraphNodesVisibility`. `newInstance` и `updateInstance` принимают/клонируют UnsignedBitSet. `getMatchedNodeId()` возвращает UnsignedBitSet.

#### platform/vcs-log/graph/src/com/intellij/vcs/log/graph/collapsing/GraphNodesVisibility.java

**BitSet type:** `UnsignedBitSet`
**Import:** `import com.intellij.vcs.log.graph.utils.UnsignedBitSet`
**Методы:** `UnsignedBitSet.get(nodeId)`, `.set(nodeId, true/false)`, getter/setter для `myNodeVisibilityById`
**Контекст:** Управление видимостью узлов графа. Хранит `UnsignedBitSet myNodeVisibilityById`. Методы `isVisible`, `show`, `hide` транслируют nodeIndex в nodeId и обращаются к UnsignedBitSet. `asFlags()` оборачивает UnsignedBitSet в `Flags`.

#### platform/vcs-log/graph/src/com/intellij/vcs/log/graph/impl/facade/FirstParentController.kt

**BitSet type:** `UnsignedBitSet`, `BitSetFlags`
**Import:** `import ...UnsignedBitSet`, `import ...BitSetFlags`
**Методы:** `UnsignedBitSet()`, `.set(...)`, `BitSetFlags(nodesCount())`
**Контекст:** Контроллер для режима «first parent only». Вычисляет видимые узлы и скрытые ребра через `getVisibleNodesAndHiddenEdges`, возвращающий `Pair<UnsignedBitSet, EdgeStorageWrapper>`. Использует `BitSetFlags` как visited.

#### platform/vcs-log/graph/src/com/intellij/vcs/log/graph/impl/facade/ReachableNodes.kt

**BitSet type:** `BitSetFlags`
**Import:** `import com.intellij.vcs.log.graph.utils.impl.BitSetFlags`
**Методы:** `BitSetFlags(graph.nodesCount())`, `.setAll(boolean)`
**Контекст:** Определение достижимых узлов для вычисления содержащих веток. `BitSetFlags` используется как `visited` для DFS-обхода; перед каждым запуском обхода сбрасывается через `setAll(false)` для переиспользования.

#### platform/vcs-log/graph/src/com/intellij/vcs/log/graph/impl/facade/sort/bek/BekBranchCreator.java

**BitSet type:** `BitSetFlags`
**Import:** `import com.intellij.vcs.log.graph.utils.impl.BitSetFlags`
**Методы:** `new BitSetFlags(permanentGraph.nodesCount(), false)`, `.get(int)`, `.set(int, boolean)`
**Контекст:** Создание BEK-веток для оптимизированной сортировки графа. `BitSetFlags myDoneNodes` — множество обработанных узлов. Активно читает (`get`) для проверки и пишет (`set`) для отметки обработанных узлов при обходе.

#### platform/vcs-log/graph/src/com/intellij/vcs/log/graph/impl/permanent/PermanentLinearGraphBuilder.java

**BitSet type:** `BitSetFlags`
**Import:** `import com.intellij.vcs.log.graph.utils.impl.BitSetFlags`
**Методы:** `new BitSetFlags(graphCommits.size())`, `.set(int, boolean)`, `.get(int)`
**Контекст:** Построение перманентного линейного графа. `BitSetFlags simpleNodes` отмечает «простые» узлы (с одним родителем и одним потомком) через `set`, затем читает при построении рёбер через `get`.

---

### 18. Platform: VCS-Log — Impl

#### platform/vcs-log/impl/src/com/intellij/vcs/log/history/FileHistoryRefiner.kt

**BitSet type:** `BitSetFlags`
**Import:** `import com.intellij.vcs.log.graph.utils.impl.BitSetFlags`
**Методы:** `BitSetFlags(nodesCount())`, `BitSetFlags(graph.nodesCount(), false)`, `.get(int)`, `.set(int, boolean)`
**Контекст:** Уточнение истории файла (переименования, перемещения). Два использования `BitSetFlags`: `visibilityBuffer` как переиспользуемый буфер для BFS, и `visited` для обхода графа при трассировке путей файла. `visited.get(...)` проверяет посещённость узла, `visited.set(..., true)` помечает узел как посещённый.

---

### 19. Plugins: Git4idea

#### plugins/git4idea/intellij.vcs.git.coverage/src/com/intellij/vcs/git/coverage/CurrentFeatureBranchBaseDetector.kt

**BitSet type:** `BitSetFlags`
**Import:** `import com.intellij.vcs.log.graph.utils.impl.BitSetFlags`
**Методы:** `BitSetFlags(graph.nodesCount())`, `.set(int, boolean)`
**Контекст:** Определение базового коммита фича-ветки для покрытия тестами. `BitSetFlags` используется как visited-множество для DFS/BFS обхода графа VCS-лога при поиске ближайшего общего предка с защищенной веткой. После каждого шага поиска visited-метки точечно сбрасываются через `set(nodeId, false)` для продолжения поиска вниз по графу.

#### plugins/git4idea/src/git4idea/history/GitHistoryTraverserImpl.kt

**BitSet type:** `BitSetFlags`
**Import:** `import com.intellij.vcs.log.graph.utils.impl.BitSetFlags`
**Методы:** `BitSetFlags(graph.nodesCount())`
**Контекст:** Обход истории Git через VCS-лог. Создает `BitSetFlags visited` для BFS/DFS обхода. Принимает лямбду-walker с сигнатурой `(startId, graph, visited: BitSetFlags, handler) -> Unit`.

#### plugins/git4idea/src/git4idea/index/GitStageLineStatusTracker.kt

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet`
**Методы:** `BitSet()`, `.set(line1, line2)`, `.nextSetBit(start)`, `getRangesForLines(BitSet)`, `rollbackChanges(BitSet)`
**Контекст:** Трекер строк для Git staging area (3-стороннее сравнение: VCS/staged/working). `BlockFilter` хранит две BitSet для фильтрации блоков по двум сторонам. `collectAffectedLines` создает BitSet из списка `StagedRange`. Реализует `LineStatusTrackerI.rollbackChanges(BitSet)`.

#### plugins/git4idea/src/git4idea/rebase/log/GitCommitEditingActionBase.kt

**BitSet type:** `BitSetFlags`
**Import:** `import com.intellij.vcs.log.graph.utils.impl.BitSetFlags`
**Методы:** `BitSetFlags(permanentGraph.linearGraph.nodesCount())`
**Контекст:** Базовый класс действий редактирования коммитов (rebase, reword). Создает `BitSetFlags used` для DFS-обхода графа при проверке, все ли выбранные коммиты принадлежат одной ветке.

---

### 20. Platform: Diff/VCS — Тесты

#### platform/diff-impl/tests/testSrc/com/intellij/diff/DiffTestCase.kt (тест)

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet`
**Методы:** `BitSet()`, `.set(index)`, `.length()`, `.get(i)`
**Контекст:** Базовый тестовый класс diff. Содержит утилиты: `assertSetsEquals(expected: BitSet, actual: BitSet)` для сравнения, `parseMatching`/`parseLineMatching` — парсинг строковых шаблонов в BitSet (символы != пробел = set bit).

#### platform/diff-impl/tests/testSrc/com/intellij/diff/comparison/BlocksComparisonUtilTest.kt (тест)

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet`
**Методы:** `BitSet()`, `.set(index)`
**Контекст:** Тесты сравнения блоков. `parseInnerExpected` парсит строку-шаблон в BitSet (символ '-' = set). `parseInnerActual` конвертирует `List<LineFragment>` в BitSet для сравнения с ожидаемым.

#### platform/diff-impl/tests/testSrc/com/intellij/diff/comparison/ComparisonMergeUtilTestBase.kt (тест)

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet`
**Методы:** `BitSet()`, `.set(start, end)`, `Trio<BitSet>` (тройка BitSet для 3-стороннего сравнения)
**Контекст:** Базовый класс тестов merge-сравнения. `checkDiffMatching` создает Trio из трех BitSet, отмечая измененные диапазоны, и сравнивает с ожидаемыми.

#### platform/diff-impl/tests/testSrc/com/intellij/diff/comparison/ComparisonUtilAutoTest.kt (тест)

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet`
**Методы:** `BitSet()`, `.set(start, end)`, `.get(offset)`, `.nextSetBit(offset)`, `.nextClearBit(offset)`
**Контекст:** Автотесты (property-based) для сравнения текстов. Создает `changesSet1`/`changesSet2`/`changesSet3` BitSet для отслеживания, какие символы помечены как измененные, затем проверяет корректность через `checkCodePoints`.

#### platform/diff-impl/tests/testSrc/com/intellij/diff/comparison/ComparisonUtilTestBase.kt (тест)

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet`
**Методы:** `BitSet()`, `.set(start, end)`
**Контекст:** Базовый класс тестов сравнения. Методы `checkLineMatching`, `checkDiffMatching`, `checkMergeMatching` создают BitSet из фрагментов и сравнивают с ожидаемыми. `Data<BitSet>` / `Couple<BitSet>` / `Trio<BitSet>` — параметризованные контейнеры.

#### platform/diff-impl/tests/testSrc/com/intellij/diff/comparison/IgnoreComparisonUtilTest.kt (тест)

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet`
**Методы:** `BitSet()`, `.set(index)`
**Контекст:** Тесты сравнения с игнорированием. `parseExpected` конвертирует строку-шаблон ('-' = set) в BitSet. `parseActual` конвертирует `List<LineFragment>` в `Couple<BitSet>` для сравнения.

#### platform/diff-impl/tests/testSrc/com/intellij/diff/tools/fragmented/UnifiedFragmentBuilderTest.kt (тест)

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet`
**Методы:** `BitSet()`, `.set(start, end)`, `.or()`, `.get(line)`
**Контекст:** Тесты построения unified-фрагментов. Наиболее интенсивное тестовое использование: `LineMapping(left: BitSet, right: BitSet, unchanged: BitSet)` — data class с тремя BitSet. Методы `processExpectedLineMapping`/`processActualLineMapping`, `processExpectedChangedLines`/`processActualChangedLines`, `processExpectedMappedLines`/`processActualMappedLines` парсят и конвертируют в BitSet для сравнения.

#### platform/vcs-impl/testSrc/com/intellij/openapi/vcs/ChangedRangesShifterAutoTest.kt (тест)

**BitSet type:** `com.intellij.util.diff.BitSet` (КАСТОМНАЯ РЕАЛИЗАЦИЯ, НЕ java.util.BitSet)
**Import:** `import com.intellij.util.diff.BitSet`
**Методы:** `BitSet()`, `.set(start, end)`, `BitSet(size, longArray)`, `.toLongArray()`, `.andNot()`, `.isEmpty`, `.toString()`
**Контекст:** Автотест сдвига измененных диапазонов. Единственный тестовый файл в этой секции, использующий кастомный `com.intellij.util.diff.BitSet` (один из 11 файлов в каталоге с этим типом; остальные — в секциях 2 и 6). Создает BitSet модифицированных строк, сдвигает и проверяет, что ничего не потерялось через `andNot`.

#### platform/vcs-tests/testSrc/com/intellij/openapi/vcs/BaseLineStatusTrackerTestCase.kt (тест)

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet`
**Методы:** *передача BitSet в `moveToChangelist(lines, list)`*
**Контекст:** Базовый тестовый класс для Line Status Tracker. Метод `moveChangesTo(lines: BitSet, list: String)` передает BitSet в `partialTracker.moveToChangelist(BitSet, changeList)`.

#### platform/vcs-tests/testSrc/com/intellij/openapi/vcs/LineStatusTrackerRevertAutoTest.kt (тест)

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet`
**Методы:** `BitSet()`, `.clear()`, `.set(line)`
**Контекст:** Автотесты отката изменений. `checkRevertComplex` создает BitSet, случайно заполняет строки и вызывает `rollbackChanges(BitSet)` для проверки корректности отката.

#### platform/vcs-tests/testSrc/com/intellij/openapi/vcs/LineStatusTrackerTestUtil.kt (тест)

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet`
**Методы:** `BitSet()`, `.set(line)`, передача в `tracker.rollbackChanges(lines)`
**Контекст:** Тестовая утилита для Line Status Tracker. `rollbackLine(line)` создает BitSet с одним битом и вызывает `rollbackLines`. `rollbackLines(BitSet)` делегирует в `tracker.rollbackChanges(BitSet)`.

#### platform/vcs-log/graph/test/com/intellij/vcs/log/graph/impl/DottedFilterEdgesGeneratorTest.kt (тест)

**BitSet type:** `UnsignedBitSet`
**Import:** `import com.intellij.vcs.log.graph.utils.UnsignedBitSet`
**Методы:** `UnsignedBitSet()`, `.set(nodeId, true)`
**Контекст:** Тесты генерации пунктирных ребер. Extension `LinearGraph.assert` создает UnsignedBitSet видимости, устанавливая биты для обычных узлов (type=USUAL).

#### platform/vcs-log/graph/test/com/intellij/vcs/log/graph/utils/BitSetFlagsTest.java (тест)

**BitSet type:** `BitSetFlags`
**Import:** `import com.intellij.vcs.log.graph.utils.impl.BitSetFlags`
**Методы:** `BitSetFlags(size)`, `.size()`, `.get(index)`, `.set(index, value)`, `.setAll(value)`
**Контекст:** Юнит-тесты `BitSetFlags`: initTest, setTest, setAllTest, size1Test, emptyFlagsTest.

#### platform/vcs-log/graph/test/com/intellij/vcs/log/graph/utils/BfsTests.kt (тест)

**BitSet type:** `BitSetFlags`
**Import:** `import com.intellij.vcs.log.graph.utils.impl.BitSetFlags`
**Методы:** `BitSetFlags(nodesCount)`, `.setAll(false)`, extension `setAll(vararg values)`
**Контекст:** Тесты BFS-обхода графа. Создает `BitSetFlags visited`, выполняет `BfsWalk.walk()`, проверяет, что посещенные узлы совпадают с ожидаемыми.

#### platform/vcs-log/graph/test/com/intellij/vcs/log/graph/utils/GraphUtilTests.kt (тест)

**BitSet type:** `BitSetFlags`
**Import:** `import com.intellij.vcs.log.graph.utils.impl.BitSetFlags`
**Методы:** `BitSetFlags(graph.nodesCount())`
**Контекст:** Тесты `getCorrespondingParent`. Создает `BitSetFlags` как visited-множество для `getCorrespondingParent(startNode, endNode, visited)`.

#### platform/vcs-log/graph/test/com/intellij/vcs/log/graph/utils/UnsignedBitSetTest.java (тест)

**BitSet type:** `UnsignedBitSet`
**Import:** (в том же пакете)
**Методы:** `UnsignedBitSet()`, `.set(index, true)`, `.set(from, to, true)`, `.get(index)`
**Контекст:** Юнит-тесты UnsignedBitSet: initFalse, setZero, setOne, setMinusOne, setSeveralTimes, setPositiveRange, setNegativeRange, setRange. Проверяют корректность работы с отрицательными и положительными индексами, включая range-set.

---

### 21. Plugins: Java-Decompiler — Exprent Hierarchy

**Общий паттерн (Shared Bytecode Offset Tracking Pattern):**

Базовый класс `Exprent` определяет паттерн отслеживания смещений байткода через `java.util.BitSet`. Каждый Exprent хранит nullable поле `bytecode` (`@Nullable BitSet`), содержащее набор смещений bytecode-инструкций, декомпилированных в данное выражение. Паттерн включает:

- **Поле:** `public @Nullable BitSet bytecode = null;`
- **`addBytecodeOffsets(BitSet)`** — объединяет переданные смещения в `bytecode` через `BitSet.or()`, лениво создавая `new BitSet()` при первом вызове.
- **`fillBytecodeRange(BitSet values)`** — абстрактный метод; каждый подкласс переопределяет его для рекурсивного сбора диапазона байткода из всех дочерних выражений.
- **`measureBytecode(BitSet values)`** — копирует собственные `bytecode` в `values` через `or()`.
- **`measureBytecode(BitSet values, Exprent)`** — делегирует к `fillBytecodeRange()` дочернего выражения.
- **`measureBytecode(BitSet values, List<Exprent>)`** — итерирует по списку, вызывая `fillBytecodeRange()` для каждого.

Все 14 подклассов переопределяют `fillBytecodeRange()`, вызывая `measureBytecode()` для своих дочерних Exprent'ов и для себя. Большинство также переопределяют `copy()` и передают `bytecode` при копировании, а также принимают `BitSet bytecodeOffsets` в конструкторе и вызывают `addBytecodeOffsets()`; исключения — `AnnotationExprent` и `AssertExprent`, которые не переопределяют `copy()` и не принимают `bytecodeOffsets` в конструкторе. Ниже указаны только отклонения от стандарта.

#### plugins/java-decompiler/engine/src/org/jetbrains/java/decompiler/modules/decompiler/exps/Exprent.java

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet;`
**Методы:** `new BitSet()`, `.or()`, поле `bytecode`, `addBytecodeOffsets(BitSet)`, `fillBytecodeRange(BitSet)` (abstract), `measureBytecode(BitSet)` (3 перегрузки)
**Контекст:** Абстрактный базовый класс для всех выражений декомпилятора. Определяет инфраструктуру хранения и агрегации bytecode-смещений через BitSet. Используется для маппинга «байткод → строка исходного кода» при генерации debug info.

#### plugins/java-decompiler/engine/src/org/jetbrains/java/decompiler/modules/decompiler/exps/AnnotationExprent.java

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet;`
**Методы:** `fillBytecodeRange(BitSet)`, `measureBytecode(BitSet, List<Exprent>)`, `measureBytecode(BitSet)` (без ctor с BitSet, без `copy()`)
**Контекст:** Стандартный паттерн Exprent. Измеряет `parValues` (значения параметров аннотации). Не принимает `bytecodeOffsets` в конструкторе.

#### plugins/java-decompiler/engine/src/org/jetbrains/java/decompiler/modules/decompiler/exps/ArrayExprent.java

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet;`
**Методы:** ctor с `BitSet bytecodeOffsets`, `addBytecodeOffsets(BitSet)`, `copy()`, `fillBytecodeRange(BitSet)`, `measureBytecode(BitSet, Exprent)` (×2), `measureBytecode(BitSet)`
**Контекст:** Стандартный паттерн Exprent. Конструктор принимает `BitSet bytecodeOffsets`. Измеряет `array` и `index`.

#### plugins/java-decompiler/engine/src/org/jetbrains/java/decompiler/modules/decompiler/exps/AssertExprent.java

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet;`
**Методы:** `fillBytecodeRange(BitSet)`, `measureBytecode(BitSet, List<Exprent>)`, `measureBytecode(BitSet)` (без ctor с BitSet, без `copy()`)
**Контекст:** Стандартный паттерн Exprent. Измеряет `parameters` (список). Не принимает `bytecodeOffsets` в конструкторе.

#### plugins/java-decompiler/engine/src/org/jetbrains/java/decompiler/modules/decompiler/exps/AssignmentExprent.java

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet;`
**Методы:** ctor с `BitSet bytecodeOffsets`, `addBytecodeOffsets(BitSet)`, `copy()`, `fillBytecodeRange(BitSet)`, `measureBytecode(BitSet, Exprent)` (×2), `measureBytecode(BitSet)`
**Контекст:** Стандартный паттерн Exprent. Конструктор принимает `BitSet bytecodeOffsets`. Измеряет `left` и `right`.

#### plugins/java-decompiler/engine/src/org/jetbrains/java/decompiler/modules/decompiler/exps/ConstExprent.java

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet;`
**Методы:** ctor с `BitSet bytecodeOffsets` (×4), `addBytecodeOffsets(BitSet)`, `copy()`, `fillBytecodeRange(BitSet)`, `measureBytecode(BitSet)`
**Контекст:** Стандартный паттерн Exprent (листовой). Четыре конструктора принимают `BitSet bytecodeOffsets`. В `fillBytecodeRange()` вызывает только `measureBytecode(values)` — нет дочерних выражений.

#### plugins/java-decompiler/engine/src/org/jetbrains/java/decompiler/modules/decompiler/exps/ExitExprent.java

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet;`
**Методы:** ctor с `BitSet bytecodeOffsets`, `addBytecodeOffsets(BitSet)`, `copy()`, `fillBytecodeRange(BitSet)`, `measureBytecode(BitSet, Exprent)`, `measureBytecode(BitSet)`
**Контекст:** Стандартный паттерн Exprent. Конструктор принимает `BitSet bytecodeOffsets`. Измеряет `value` (return/throw expression).

#### plugins/java-decompiler/engine/src/org/jetbrains/java/decompiler/modules/decompiler/exps/FieldExprent.java

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet;`
**Методы:** ctor с `BitSet bytecodeOffsets` (×2), `addBytecodeOffsets(BitSet)`, `copy()`, `fillBytecodeRange(BitSet)`, `measureBytecode(BitSet, Exprent)`, `measureBytecode(BitSet)`
**Контекст:** Стандартный паттерн Exprent. Два конструктора принимают `BitSet bytecodeOffsets`. Измеряет `instance`.

#### plugins/java-decompiler/engine/src/org/jetbrains/java/decompiler/modules/decompiler/exps/FunctionExprent.java

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet;`
**Методы:** ctor с `BitSet bytecodeOffsets` (×3), `addBytecodeOffsets(BitSet)`, `copy()`, `fillBytecodeRange(BitSet)`, `measureBytecode(BitSet, List<Exprent>)`, `measureBytecode(BitSet)`
**Контекст:** Стандартный паттерн Exprent. Три конструктора принимают `BitSet bytecodeOffsets`. Измеряет `lstOperands`.

#### plugins/java-decompiler/engine/src/org/jetbrains/java/decompiler/modules/decompiler/exps/IfExprent.java

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet;`
**Методы:** ctor с `BitSet bytecodeOffsets` (×2), `addBytecodeOffsets(BitSet)`, `copy()`, `fillBytecodeRange(BitSet)`, `measureBytecode(BitSet, Exprent)`, `measureBytecode(BitSet)`
**Контекст:** Стандартный паттерн Exprent. Конструктор принимает `BitSet bytecodeOffsets`. Измеряет `condition`. Дополнительно: в `negateIf()` передаёт `condition.bytecode` при создании нового `FunctionExprent`.

#### plugins/java-decompiler/engine/src/org/jetbrains/java/decompiler/modules/decompiler/exps/InvocationExprent.java

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet;`
**Методы:** Стандартный паттерн Exprent + **дополнительное использование**: `private static final BitSet EMPTY_BIT_SET = new BitSet(0)`, `new BitSet(descriptor.params.length)`, `.set(int)`, `.get(int)`, `.isEmpty()`
**Контекст:** Помимо стандартного паттерна, содержит **отдельное самостоятельное использование BitSet** в методе `getAmbiguousParameters()`. Создаёт `BitSet ambiguous` размером в количество параметров метода; для каждого параметра проверяет неоднозначность при перегруженных методах. Бит устанавливается через `.set(i)`, читается через `.get(i)` в `toJava()` для определения необходимости явного cast. Константа `EMPTY_BIT_SET` используется при отсутствии неоднозначности.

#### plugins/java-decompiler/engine/src/org/jetbrains/java/decompiler/modules/decompiler/exps/MonitorExprent.java

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet;`
**Методы:** ctor с `BitSet bytecodeOffsets`, `addBytecodeOffsets(BitSet)`, `copy()`, `fillBytecodeRange(BitSet)`, `measureBytecode(BitSet, Exprent)`, `measureBytecode(BitSet)`
**Контекст:** Стандартный паттерн Exprent. Конструктор принимает `BitSet bytecodeOffsets`. Измеряет `value`.

#### plugins/java-decompiler/engine/src/org/jetbrains/java/decompiler/modules/decompiler/exps/NewExprent.java

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet;`
**Методы:** ctor с `BitSet bytecodeOffsets` (×2), `copy()`, `fillBytecodeRange(BitSet)`, `measureBytecode(BitSet, List<Exprent>)` (×2), `measureBytecode(BitSet, Exprent)`, `measureBytecode(BitSet)`
**Контекст:** Стандартный паттерн Exprent. Два конструктора принимают `BitSet bytecodeOffsets`. В `fillBytecodeRange()` измеряет `lstArrayElements`, `lstDims`, `constructor` — расширенный набор дочерних элементов.

#### plugins/java-decompiler/engine/src/org/jetbrains/java/decompiler/modules/decompiler/exps/SwitchExprent.java

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet;`
**Методы:** ctor с `BitSet bytecodeOffsets`, `addBytecodeOffsets(BitSet)`, `copy()`, `fillBytecodeRange(BitSet)` (нестандартная реализация), `measureBytecode(BitSet, Exprent)`, `measureBytecode(BitSet)`
**Контекст:** Стандартный паттерн Exprent с **нестандартной реализацией `fillBytecodeRange()`**: вручную итерирует по `List<List<Exprent>> caseValues`, проверяя на null каждый элемент. Также измеряет `value`.

#### plugins/java-decompiler/engine/src/org/jetbrains/java/decompiler/modules/decompiler/exps/VarExprent.java

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet;`
**Методы:** ctor с `BitSet bytecode`, `copy()`, `fillBytecodeRange(BitSet)`, `measureBytecode(BitSet)`
**Контекст:** Стандартный паттерн Exprent (листовой, как ConstExprent). Конструктор принимает `BitSet bytecode`.

---

### 22. Plugins: Java-Decompiler — Statement Hierarchy

**Общий паттерн:** базовый класс `Statement` определяет метод `getOffset(BitSet values)`, который рекурсивно собирает bytecode-смещения из всех Exprent'ов и дочерних Statement'ов. Метод `getStartEndRange()` создаёт `new BitSet()`, вызывает `getOffset()` и извлекает диапазон через `nextSetBit(0)` и `length() - 1`.

#### plugins/java-decompiler/engine/src/org/jetbrains/java/decompiler/modules/decompiler/stats/Statement.java

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet;`
**Методы:** `new BitSet()`, `.or()`, `.nextSetBit(int)`, `.length()`, параметр `@Nullable BitSet values` в `getOffset()`
**Контекст:** Абстрактный базовый класс для statement'ов. `getOffset(BitSet values)` рекурсивно собирает bytecode-смещения. `getStartEndRange()` создаёт BitSet, заполняет через `getOffset()` и возвращает `(nextSetBit(0), length() - 1)`.

#### plugins/java-decompiler/engine/src/org/jetbrains/java/decompiler/modules/decompiler/stats/CatchStatement.java

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet;`
**Методы:** параметр `BitSet values` в `getOffset(BitSet)`
**Контекст:** Переопределяет `getOffset()`: вызывает `super.getOffset(values)`, затем итерирует `getResources()` (try-with-resources) и вызывает `fillBytecodeRange()` для каждого ресурса.

#### plugins/java-decompiler/engine/src/org/jetbrains/java/decompiler/modules/decompiler/stats/DummyExitStatement.java

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet;`
**Методы:** `new BitSet()`, `.or()`, `.isEmpty()`, поле `@Nullable BitSet bytecode`
**Контекст:** Фиктивный exit-statement. Имеет собственное nullable поле `bytecode` и метод `addBytecodeOffsets(BitSet)` (аналог Exprent). Проверка `.isEmpty()` перед объединением.

---

### 23. Plugins: Java-Decompiler — Прочее

#### plugins/java-decompiler/engine/src/org/jetbrains/java/decompiler/modules/decompiler/ConcatenationHelper.java

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet;`
**Методы:** параметр `@Nullable BitSet bytecode` в `createConcatExprent()`
**Контекст:** Упрощение конкатенации строк. Метод `createConcatExprent()` принимает `BitSet bytecode` и передаёт в новые `FunctionExprent`. BitSet проходит сквозь метод без модификации.

#### plugins/java-decompiler/engine/src/org/jetbrains/java/decompiler/modules/decompiler/ExprProcessor.java

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet;`
**Методы:** `new BitSet()`, `.set(int)`, `.set(int, int)` (range set)
**Контекст:** Главный процессор, транслирующий bytecode в дерево Exprent'ов. Создаёт `BitSet offsets` для каждой инструкции, устанавливает текущий offset и передаёт в конструкторы Exprent'ов. Является **точкой входа** для создания bytecode-mapping BitSet'ов.

#### plugins/java-decompiler/engine/src/org/jetbrains/java/decompiler/modules/decompiler/SimplifyExprentsHelper.java

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet;`
**Методы:** чтение поля `Exprent.bytecode` (тип `BitSet`)
**Контекст:** Упрощение выражений. В `buildIff()` читает `ifHeadExpr.bytecode` и передаёт при трансформации. BitSet используется как read-only носитель bytecode-смещений.

#### plugins/java-decompiler/engine/src/org/jetbrains/java/decompiler/modules/decompiler/vars/VarProcessor.java

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet;`
**Методы:** `new BitSet()`, `.get(int)`, `.set(int)`
**Контекст:** Процессор переменных. Поле `finalParameters = new BitSet()` хранит множество индексов параметров метода, объявленных как `final`. `.set(pair.var)` — пометка, `.get(pair.var)` — проверка.

#### plugins/java-decompiler/engine/src/org/jetbrains/java/decompiler/struct/attr/StructLocalVariableTableAttribute.java

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet;`
**Методы:** `new BitSet()`, `.nextSetBit(int)`, `.length()`
**Контекст:** Атрибут `LocalVariableTable` из class-файла. Метод `matchingVars(Statement stat)` создаёт `new BitSet()`, заполняет через `stat.getOffset(values)`, извлекает диапазон `(nextSetBit(0), length() - 1)` и фильтрует локальные переменные.

#### plugins/java-decompiler/engine/src/org/jetbrains/java/decompiler/struct/consts/ConstantPool.java

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet;`
**Методы:** `new BitSet(int)`, `.set(int)`, `.nextSetBit(int)`, массив `BitSet[]`
**Контекст:** Парсер constant pool. Массив из 3 BitSet'ов (`nextPass[0..2]`) для многопроходного разрешения ссылок. Итерация через `nextSetBit(idx + 1)`.

#### plugins/java-decompiler/engine/src/org/jetbrains/java/decompiler/main/collectors/BytecodeMappingTracer.java

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet;`
**Методы:** параметр `@Nullable BitSet bytecode_offsets`, `.nextSetBit(int)` для итерации
**Контекст:** Трейсер маппинга «bytecode offset → source line». Метод `addMapping(BitSet)` итерирует по всем установленным битам через `nextSetBit(0)` / `nextSetBit(i+1)`. Конечный потребитель BitSet'ов из иерархии Exprent.

#### plugins/java-decompiler/engine/src/org/jetbrains/java/decompiler/main/rels/LambdaProcessor.java

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet;`
**Методы:** `new BitSet()`, `.set(int)`, `.get(int)`, `.isEmpty()`
**Контекст:** Процессор лямбда-выражений. `BitSet lambdaMethods` отслеживает индексы bootstrap-методов, являющихся лямбда-фабриками. Выход по `.isEmpty()`.

#### plugins/java-decompiler/engine/src/org/jetbrains/java/decompiler/util/DebugPrinter.java

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet;`
**Методы:** `new BitSet()`, `.nextSetBit(int)`, `.length()`
**Контекст:** Отладочная утилита. Создаёт `new BitSet()`, вызывает `statement.getOffset(values)` или `exp.fillBytecodeRange(values)`, извлекает диапазон и выводит в `System.out`.

---

### 24. Java: Analysis-Impl — Contract/Dataflow Inference

#### java/java-analysis-impl/src/com/intellij/codeInspection/dataFlow/inference/ContractInferenceIndex.kt

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet`
**Методы:** `.isEmpty`
**Контекст:** Параметр `notNullParams: BitSet` в функции `createData()`. BitSet хранит маску индексов параметров метода, которые гарантированно не null. Проверяется через `notNullParams.isEmpty` (Kotlin property-доступ к `isEmpty()`), затем передаётся в `MethodData`.

#### java/java-analysis-impl/src/com/intellij/codeInspection/dataFlow/inference/ContractInferenceInterpreter.java

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet;`
**Методы:** `new BitSet()`, `.set(int)`, `.get(int)`
**Контекст:** Внутренний класс `ReturnValueVisitor` использует `BitSet assignedParameters` для отслеживания параметров, которым было присвоено значение. Параметры с присвоенным значением не могут быть return-значениями контракта.

#### java/java-analysis-impl/src/com/intellij/codeInspection/dataFlow/inference/JavaSourceInference.java

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet;`
**Методы:** `new BitSet()`, `.isEmpty()`, `.get(int)`
**Контекст:** Record `MethodInferenceData` содержит `notNullParameters: BitSet`. Метод `infer()` извлекает BitSet из `MethodData`, `inferNullability(PsiParameter)` проверяет конкретный бит через `.get(index)`.

#### java/java-analysis-impl/src/com/intellij/codeInspection/dataFlow/inference/MethodDataExternalizer.kt

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet`
**Методы:** `.toByteArray()`, `BitSet.valueOf(bytes)` (static factory)
**Контекст:** Сериализация/десериализация `MethodData` для Gist-индекса. Методы `writeBitSet()` и `readBitSet()` преобразуют BitSet в байтовый массив и обратно. Ограничение: до 255 байт (2040 бит).

#### java/java-analysis-impl/src/com/intellij/codeInspection/dataFlow/inference/ParameterNullityInference.kt

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet`
**Методы:** `BitSet()`, `.set(int)`
**Контекст:** Функция `inferNotNullParameters()` анализирует тело метода по Light AST, определяя параметры, обязательно разыменовываемые до проверки на null. Результат — `BitSet` с индексами not-null параметров.

#### java/java-analysis-impl/src/com/intellij/codeInspection/dataFlow/inference/inferenceResults.kt

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet`
**Методы:** (поле в data class)
**Контекст:** Data class `MethodData` содержит `val notNullParameters: BitSet`. Центральная структура данных inference-подсистемы: создаётся в `ContractInferenceIndex`, заполняется через `ParameterNullityInference`, сериализуется через `MethodDataExternalizer`, используется в `JavaSourceInference`.

#### java/java-analysis-impl/src/com/intellij/codeInspection/bytecodeAnalysis/ProjectBytecodeAnalysis.java

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet;`
**Методы:** `BitSet()`, `.cardinality()`, `.get(int)`, `IntStreamEx...toBitSet()` (StreamEx)
**Контекст:** Анализ байткода для определения not-null параметров. Метод `findAlwaysNotNullParameters()` принимает `BitSet possiblyNotNullParameters`, возвращает `BitSet alwaysNotNullParameters`.

### 25. Java: Analysis-Impl — Inspections

#### java/java-analysis-impl/src/com/siyeh/ig/controlflow/DuplicateConditionInspection.java

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet;`
**Методы:** `BitSet()`, `.get(int)`, `.set(int)`
**Контекст:** Инспекция дублирующихся условий в if-else цепочках. `BitSet matched` отслеживает индексы уже сопоставленных (эквивалентных) условий.

#### java/java-analysis-impl/src/com/siyeh/ig/migration/TryFinallyCanBeTryWithResourcesInspection.java

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet;`
**Методы:** `BitSet(int)`, `.set(int, boolean)`, `.get(int)`
**Контекст:** Миграция try-finally в try-with-resources. `BitSet closedVariableStatementIndices` отслеживает, какие statement'ы в finally — вызовы `.close()` на AutoCloseable.

#### java/java-analysis-impl/src/com/siyeh/ig/psiutils/ControlFlowUtils.java

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet;`
**Методы:** `BitSet()`, `.get(int)`, `.set(int)`, `.set(int, boolean)`
**Контекст:** Утилита анализа потока управления. BitSet `referenced` отслеживает инструкции, достижимые из точки доступа к переменной.

### 26. Java: PSI-Impl (Control Flow)

#### java/java-psi-impl/src/com/intellij/psi/controlFlow/ControlFlowUtil.java

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet;`
**Методы:** `BitSet(int)`, `.set(int, boolean)`, `.get(int)`, `.clear()`, `.or(BitSet)`
**Контекст:** Центральная утилита анализа потока управления Java-кода. BitSet используется: (1) `getReachableInstructionsCalculator()` — множество достижимых инструкций через DFS, `collectedOffsets.or(reachableOffsets)` для объединения reachability-наборов, (2) `isDominator()` — `myReachedWithoutDominator` для проверки доминирования, (3) `depthFirstSearch()` — `visitedOffsets` для обхода.

#### java/java-psi-impl/src/com/intellij/psi/controlFlow/DefUseUtil.java

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet;`
**Методы:** `BitSet(int)`, `.get(int)`, `.set(int)`
**Контекст:** Анализ определений и использований переменных (def-use). BitSet `usefulWrites` отмечает индексы инструкций записи, результат которых впоследствии читается.

#### java/java-psi-impl/src/com/intellij/psi/impl/source/tree/java/ClassElement.java

**BitSet type:** **False positive** — файл НЕ использует `java.util.BitSet`. Переменные с `BIT_SET` в имени — это экземпляры `com.intellij.psi.tree.TokenSet`, не `java.util.BitSet`.

### 27. Java: Impl и Прочее

**java/java-impl** (2 файла)

#### java/java-impl/src/com/intellij/codeInsight/javadoc/SnippetMarkup.java

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet;`
**Методы:** `BitSet()`, `.set(int, int)`, `.nextSetBit(int)`
**Контекст:** Обработка Javadoc `@snippet`. Поле `myTextOffsets: BitSet` хранит диапазоны plain-текста в сниппете. `isTextPart(TextRange)` проверяет через `nextSetBit()`.

#### java/java-impl/src/com/siyeh/ig/performance/CollectionsMustHaveInitialCapacityInspection.java

**BitSet type:** **False positive** — строковая ссылка на `java.util.BitSet`
**Import:** Нет импорта `java.util.BitSet`
**Методы:** Нет прямого использования API
**Контекст:** Инспекция проверяет наличие начальной capacity при создании коллекций. `"java.util.BitSet"` хранится в списке отслеживаемых классов как строковый FQN. Это семантическая ссылка на тип, а не использование `BitSet` API.

**java/java-impl-inspections** (1 файл)

#### java/java-impl-inspections/src/com/intellij/codeInspection/defUse/DefUseInspection.java

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet;`
**Методы:** `BitSet()`, `.get(int)`, `.set(int)`, `.stream().toArray()`
**Контекст:** Инспекция неиспользуемых присваиваний. `BitSet untilAssignment` для обхода CFG до первого присваивания полю. `.stream().toArray()` извлекает индексы инструкций.

**java/java-impl-refactorings** (1 файл)

#### java/java-impl-refactorings/src/com/intellij/refactoring/extractMethod/ExtractMethodRecommenderInspection.java

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet;`
**Методы:** `BitSet()`, `.isEmpty()`, `.set(int)`, `.nextSetBit(int)`
**Контекст:** Рекомендатель Extract Method. BitSet помечает индексы statement'ов-объявлений. `.nextSetBit(from)` проверяет наличие объявлений в диапазоне.

**java/idea-ui** (1 файл)

#### java/idea-ui/src/com/intellij/ide/util/importProject/RootDetectionProcessor.java

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet;`
**Методы:** `BitSet(int)`, `.set(int, int)`, `.get(int)`, `.set(int, boolean)`, `.or(BitSet)`, `.isEmpty()`
**Контекст:** Обнаружение корней проекта при импорте. `BitSet enabledDetectors` отслеживает активные `ProjectStructureDetector` при рекурсивном обходе.

### 28. Java: Тесты

#### java/java-tests/testSrc/com/siyeh/ig/performance/CollectionsMustHaveInitialCapacityInspectionTest.java (тест)

**BitSet type:** **False positive** — строковые тестовые сниппеты с `java.util.BitSet`
**Import:** Нет импорта `java.util.BitSet`
**Методы:** Нет прямого использования API в коде теста; упоминания есть только внутри строковых Java-сниппетов
**Контекст:** (тест) Тест для `CollectionsMustHaveInitialCapacityInspection`. Определяет мок-класс `java.util.BitSet` и использует строковые Java-сниппеты с `new BitSet()` / `new BitSet(int)` для проверки инспекции. Это тестовые данные, а не вызовы `BitSet` API из кода теста.

---

### 29. Plugins: Groovy — Dataflow Analysis

#### plugins/groovy/groovy-psi/src/org/jetbrains/plugins/groovy/lang/psi/dataFlow/WorkList.kt

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet`
**Методы:** `BitSet()`, `.set(Int, Int)`, `.isEmpty`, `.nextSetBit()`, `.clear()`, `.set(Int, Boolean)`
**Контекст:** Рабочий список для обхода инструкций графа потока данных. `BitSet` (`mySet`) — компактное множество индексов инструкций, ожидающих обработки. `nextSetBit`/`clear` — извлечение следующего элемента, `set` — добавление.

#### plugins/groovy/groovy-psi/src/org/jetbrains/plugins/groovy/lang/psi/dataFlow/readWrite/ReadBeforeWriteState.kt

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet`
**Методы:** `BitSet()`, `.clone()`, `.toString()`
**Контекст:** Состояние DFA «чтение до записи». Два поля `writes: BitSet` и `reads: BitSet`, где `writes` — множество переменных с записью, `reads` — множество инструкций чтения без предшествующей записи. Глубокое клонирование обоих BitSet.

#### plugins/groovy/groovy-psi/src/org/jetbrains/plugins/groovy/lang/psi/dataFlow/readWrite/ReadBeforeWriteInstance.kt

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet`
**Методы:** `.clone()`, `.set()`, `.get()`
**Контекст:** Экземпляр DFA для анализа чтений без предшествующей записи. Клонирует `BitSet` из `ReadBeforeWriteState`, `.set(nameId)` для пометки записанных переменных, `.get(nameId)` для проверки.

#### plugins/groovy/groovy-psi/src/org/jetbrains/plugins/groovy/lang/psi/dataFlow/util.kt

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet`
**Методы:** `BitSet()`, `.set()`, возвращаемый тип `BitSet`
**Контекст:** Утилитная функция `getSimpleInstructions()` — определяет инструкции вне циклов. Результат — `BitSet`, используется в `InferenceCache`.

#### plugins/groovy/groovy-psi/src/org/jetbrains/plugins/groovy/lang/psi/dataFlow/types/TypeDfaState.java

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet`
**Методы:** `BitSet()`, `.get()`, `.set(int, boolean)`, `.clone()`, `.or()`, `.equals()`, `.isEmpty()`
**Контекст:** Состояние DFA для типизации Groovy. Поле `myProhibitedCachingVars: BitSet` — множество дескрипторов переменных, чьи типы стёрты и не кэшируются. `merge` использует `clone().or()`.

#### plugins/groovy/groovy-psi/src/org/jetbrains/plugins/groovy/lang/psi/dataFlow/types/InferenceCache.java

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet`
**Методы:** `.get()` (через `Lazy<BitSet>`)
**Контекст:** Кэш вывода типов. `Lazy<BitSet> simpleInstructions` из `getSimpleInstructions()`. В `publishDescriptor()` проверяет `.get(instruction.num())` — является ли инструкция ациклической.

#### plugins/groovy/groovy-psi/src/org/jetbrains/plugins/groovy/codeInspection/utils/ControlFlowUtils.java

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet`
**Методы:** `BitSet(int)`, `.or()`, `.set(int)`, `.nextSetBit()`, `.cardinality()`, `.get(int)`
**Контекст:** Утилиты анализа потока управления. `inferWriteAccessMap()` возвращает `List<BitSet>`, где каждый BitSet хранит номера инструкций записи, достигающих данной точки. Используется `Semilattice<BitSet>` с `.or()`.

#### plugins/groovy/groovy-psi/src/org/jetbrains/plugins/groovy/lang/psi/controlFlow/ControlFlowBuilderUtil.java

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet`
**Методы:** `.nextSetBit()`
**Контекст:** `getReadsWithoutPriorWrites()` получает `BitSet reads` и итерирует через `nextSetBit()`.

---

### 30. Plugins: Groovy — Refactoring

#### plugins/groovy/src/org/jetbrains/plugins/groovy/refactoring/inline/GroovyInlineLocalHandler.java

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet`
**Методы:** `.cardinality()`, `.nextSetBit()`, `.get(int)` (через `List<BitSet>`)
**Контекст:** Инлайнинг локальных переменных. Получает `List<BitSet> writes` из `inferWriteAccessMap()`, для текущей инструкции `prev = writes.get(instruction.num())`. Если `prev.cardinality() == 1` — единственная запись через `prev.nextSetBit(0)`.

#### plugins/groovy/src/org/jetbrains/plugins/groovy/refactoring/inline/GroovyInlineLocalProcessor.java

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet`
**Методы:** `.cardinality()`, `.get(int)`, `.nextSetBit()` (через `List<BitSet>`)
**Контекст:** Процессор инлайнинга. `prev.cardinality() == 1 && prev.get(writeInstructionNumber)` — проверка, что чтение зависит от одной конкретной записи.

#### plugins/groovy/src/org/jetbrains/plugins/groovy/refactoring/introduce/parameter/GrIntroduceClosureParameterProcessor.java

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet`
**Методы:** `.cardinality()`, `.nextSetBit()`, `.get(int)` (через `List<BitSet>`)
**Контекст:** Введение параметра замыкания. Тот же паттерн `inferWriteAccessMap()` + `cardinality() == 1`.

---

### 31. Plugins: Kotlin

#### plugins/kotlin/code-insight/fixes-k2/src/org/jetbrains/kotlin/idea/k2/codeinsight/fixes/ChangeMemberFunctionSignatureFixFactory.kt

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet`
**Методы:** `BitSet(Int)`, оператор `[]` (get/set)
**Контекст:** K2-версия фикса для ошибки `NOTHING_TO_OVERRIDE`. Два `BitSet` — `matched` и `used` — для сопоставления параметров текущей функции с параметрами суперфункции (двухпроходный: сначала по именам, затем по типам).

#### plugins/kotlin/idea/src/org/jetbrains/kotlin/idea/quickfix/ChangeMemberFunctionSignatureFix.kt

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet`
**Методы:** `BitSet(Int)`, оператор `[]` (get/set)
**Контекст:** K1-версия (устаревшая, `@K1Deprecation`) того же фикса. Идентичная логика `matched`/`used` BitSet.

#### plugins/kotlin/idea/src/org/jetbrains/kotlin/idea/refactoring/move/moveDeclarations/ui/MoveKotlinNestedClassesToUpperLevelDialog.java

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet`
**Методы:** `BitSet(Int)`, `.set(int, boolean)`, `.equals()`
**Контекст:** Диалог перемещения вложенных классов. `BitSet(3)` хранит состояние трёх чекбоксов (searchInComments, searchForText, passOuterClass). Сравнение `.equals()` для FUS-телеметрии.

#### plugins/kotlin/idea/src/org/jetbrains/kotlin/idea/refactoring/move/moveDeclarations/ui/MoveKotlinTopLevelDeclarationsDialog.java

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet`
**Методы:** `BitSet(Int)`, `.set(int, boolean)`, `.equals()`
**Контекст:** Диалог перемещения top-level деклараций. `BitSet(5)` для пяти чекбоксов. Сравнение `.equals()` для FUS-телеметрии.

#### plugins/kotlin/jvm-debugger/sequence/src/org/jetbrains/kotlin/idea/debugger/sequence/psi/java/StreamExCallChecker.kt

**BitSet type:** **False positive** — нет использования `java.util.BitSet`. Только строковая ссылка `"toBitSet"` в списке терминальных операций StreamEx.

---

### 32. Plugins: Сгенерированные лексеры (JFlex)

**Общий паттерн JFlex:** поле `private BitSet zzFin = null` хранит множество позиций, являющихся допускающими состояниями в обратном DFA для обработки lookahead. Прямой проход: `.set(pos, boolean)`, обратный проход: `.get(pos)`. Также `.clear(pos)`, `.size()`. Код полностью сгенерирован JFlex.

#### plugins/yaml/gen/_YAMLLexer.java

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet;`
**Методы:** `BitSet(int)`, `.set(int, boolean)`, `.get(int)`, `.clear(int)`, `.size()`
**Контекст:** Сгенерированный JFlex-лексер YAML. 7+ блоков lookahead с `zzFin: BitSet`.

#### plugins/xpath/xpath-lang/gen/org/intellij/lang/xpath/_XPathLexer.java

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet;`
**Методы:** `BitSet(int)`, `.set(int, boolean)`, `.get(int)`, `.clear(int)`, `.size()`
**Контекст:** Сгенерированный JFlex-лексер XPath. 1 блок lookahead.

### 33. Сгенерированный код: Thrift (python/gen)

Все 12 файлов в `python/gen/com/jetbrains/python/console/protocol/` сгенерированы **Apache Thrift Compiler (0.20.0)**, дата генерации 2025-06-03. Каждый файл содержит идентичный паттерн использования `java.util.BitSet` в inner-классе `TupleScheme`:

- **Запись:** `java.util.BitSet optionals = new java.util.BitSet()` -> `optionals.set(N)` для каждого установленного optional-поля -> `oprot.writeBitSet(optionals, <fieldCount>)`
- **Чтение:** `java.util.BitSet incoming = iprot.readBitSet(<fieldCount>)` -> `incoming.get(N)` для проверки каждого поля

Это часть **Thrift TupleScheme** — компактного двоичного протокола сериализации, где BitSet кодирует, какие из optional-полей присутствуют в сообщении.

| Класс | Опциональных полей |
|---|---|
| `ArrayData` | 3 |
| `ArrayHeaders` | 2 |
| `ColHeader` | 5 |
| `CompletionOption` | 4 |
| `DebugValue` | 11 |
| `GetArrayResponse` | 9 |
| `PythonConsoleBackendService` | множественные inner-классы (args/result для ~30 RPC-методов, от 1 до 6 полей каждый) |
| `PythonConsoleFrontendService` | множественные inner-классы (args/result для ~8 RPC-методов, от 1 до 2 полей каждый) |
| `PythonTableException` | 1 |
| `PythonUnhandledException` | 1 |
| `RowHeader` | 1 |
| `UnsupportedArrayTypeException` | 1 |

**Файлы (полные repo-relative пути):**
- `python/gen/com/jetbrains/python/console/protocol/ArrayData.java`
- `python/gen/com/jetbrains/python/console/protocol/ArrayHeaders.java`
- `python/gen/com/jetbrains/python/console/protocol/ColHeader.java`
- `python/gen/com/jetbrains/python/console/protocol/CompletionOption.java`
- `python/gen/com/jetbrains/python/console/protocol/DebugValue.java`
- `python/gen/com/jetbrains/python/console/protocol/GetArrayResponse.java`
- `python/gen/com/jetbrains/python/console/protocol/PythonConsoleBackendService.java`
- `python/gen/com/jetbrains/python/console/protocol/PythonConsoleFrontendService.java`
- `python/gen/com/jetbrains/python/console/protocol/PythonTableException.java`
- `python/gen/com/jetbrains/python/console/protocol/PythonUnhandledException.java`
- `python/gen/com/jetbrains/python/console/protocol/RowHeader.java`
- `python/gen/com/jetbrains/python/console/protocol/UnsupportedArrayTypeException.java`

**BitSet type:** `java.util.BitSet`
**Import:** Нет явного — используется полностью квалифицированное имя `java.util.BitSet`
**Методы:** `new BitSet()`, `.set(int)`, `.get(int)`, `oprot.writeBitSet(BitSet, int)`, `iprot.readBitSet(int)`
**Контекст:** Сгенерированный код протокола Thrift для Python Console — коммуникация между IntelliJ и Python-процессом. BitSet используется исключительно как механизм сериализации наличия optional-полей в компактном формате TupleScheme. Код не должен модифицироваться вручную.

---

### 34. Python: PSI

#### python/python-psi-api/src/com/jetbrains/python/psi/stubs/PyFileStub.java

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet;`
**Методы:** `getFutureFeatures()` -> `BitSet`
**Контекст:** Интерфейс stub-а для Python-файлов. Объявляет метод `getFutureFeatures()`, возвращающий `BitSet` с набором `from __future__ import ...` фич, обнаруженных в Python-файле. Используется системой индексации IntelliJ для быстрого доступа к метаданным без полного парсинга.

---

#### python/python-psi-impl/src/com/jetbrains/python/psi/PyFileElementType.java

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet;`
**Методы:** `new BitSet(32)`, `.get(int)`, `.set(int, boolean)`, `readBitSet(StubInputStream)` -> `BitSet`, `writeBitSet(StubOutputStream, BitSet)`
**Контекст:** Тип элемента для Python-файлов в PSI-дереве. Реализует сериализацию/десериализацию `BitSet` в stub-поток. Метод `readBitSet()` читает 32 бита из `int`, побитово заполняет `BitSet(32)`. Метод `writeBitSet()` собирает все 32 бита `BitSet` обратно в `int` через побитовый OR. Комментарий в коде: *«here we assume that bitset has no more than 32 bits so that the value fits into an int»*. Ручная побитовая сериализация — потенциальный кандидат на замену стандартным BitSet API.

---

#### python/python-psi-impl/src/com/jetbrains/python/psi/impl/stubs/PyFileStubImpl.java

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet;`
**Методы:** `new BitSet(32)`, `.set(int, boolean)`
**Контекст:** Реализация stub-а для Python-файлов. Хранит `myFutureFeatures: BitSet` с размером `FUTURE_FEATURE_SET_SIZE = 32` (комментарий: *«32 features is ought to be enough for everybody! all bits fit into an int»*). При создании из живого файла (`PyFileImpl`) итерирует `FutureFeature.values()` и устанавливает бит по `ordinal()` каждой фичи через `myFutureFeatures.set(fuf.ordinal(), fileImpl.calculateImportFromFuture(fuf))`. Классический паттерн «enum ordinals как индексы BitSet».

---

### 35. Updater: MSLinks (BitSet32)

#### updater/src/mslinks/data/BitSet32.java

**BitSet type:** Кастомная реализация (32-битная)
**Import:** `import mslinks.io.ByteReader;`
**Определяемые методы:**
- `BitSet32(ByteReader)` — конструктор, читает 4 байта из потока как `int`
- `get(int i)` -> `boolean` (protected) — проверяет бит `(d & (1 << i)) != 0`
- `clear(int i)` (protected) — сбрасывает бит `d &= ~(1 << i)`

**Внутреннее хранилище:** `private int d` — одно 32-битное слово.
**Контекст:** Минимальная 32-битная BitSet-реализация из библиотеки [mslinks](https://github.com/DmitriiShamrikov/mslinks) (парсинг Windows `.lnk` файлов). Базовый класс для `LinkFlags`, `LinkInfoFlags`, `CNRLinkFlags`. Предоставляет только чтение и сброс отдельных бит — нет `set()`, нет итерации, нет побитовых операций. Лицензия WTFPL.

---

#### updater/src/mslinks/data/CNRLinkFlags.java

**BitSet type:** `mslinks.data.BitSet32` (наследует)
**Import:** `import mslinks.io.ByteReader;`
**Методы:** `CNRLinkFlags(ByteReader)`, `isValidDevice()` -> `get(0)`, `isValidNetType()` -> `get(1)`, `reset()` — сбрасывает биты 2..31
**Контекст:** Флаги Common Network Relative Link в `.lnk`-файлах Windows. Наследует `BitSet32`, определяет 2 именованных флага. Метод `reset()` в конструкторе обнуляет неиспользуемые биты (позиции 2-31) для защиты от мусорных данных в файле.

---

#### updater/src/mslinks/data/LinkFlags.java

**BitSet type:** `mslinks.data.BitSet32` (наследует)
**Import:** `import mslinks.io.ByteReader;`
**Методы:** `LinkFlags(ByteReader)`, `reset()`, 25 именованных геттеров: `hasLinkTargetIDList()` (бит 0), `hasLinkInfo()` (1), `hasName()` (2), `hasRelativePath()` (3), `hasWorkingDir()` (4), `hasArguments()` (5), `hasIconLocation()` (6), `isUnicode()` (7), `forceNoLinkInfo()` (8), `hasExpString()` (9), `runInSeparateProcess()` (10), `hasDarwinID()` (12), `runAsUser()` (13), `hasExpIcon()` (14), `noPidlAlias()` (15), `runWithShimLayer()` (17), `forceNoLinkTrack()` (18), `enableTargetMetadata()` (19), `disableLinkPathTracking()` (20), `disableKnownFolderTracking()` (21), `disableKnownFolderAlias()` (22), `allowLinkToLink()` (23), `unaliasOnSave()` (24), `preferEnvironmentPath()` (25), `keepLocalIDListForUNCTarget()` (26)
**Контекст:** Основные флаги Shell Link (`.lnk`) файла. Самый крупный наследник `BitSet32` — 25 из 32 бит используются. Биты 11, 16 и 27-31 сбрасываются в `reset()` как зарезервированные. Каждый флаг определяет наличие секции или поведение ярлыка Windows. Паттерн: enum-подобные именованные флаги поверх BitSet.

---

#### updater/src/mslinks/data/LinkInfoFlags.java

**BitSet type:** `mslinks.data.BitSet32` (наследует)
**Import:** `import mslinks.io.ByteReader;`
**Методы:** `LinkInfoFlags(ByteReader)`, `reset()` — сбрасывает биты 2..31, `hasVolumeIDAndLocalBasePath()` -> `get(0)`, `hasCommonNetworkRelativeLinkAndPathSuffix()` -> `get(1)`
**Контекст:** Флаги секции LinkInfo в `.lnk`-файлах. Аналогично `CNRLinkFlags` — 2 именованных флага, остальные биты сбрасываются.

---

### 36. Fleet: Util (fleet.util.BitSet)

#### fleet/util/core/srcCommonMain/fleet/util/BitSet.kt

**BitSet type:** Кастомная реализация (Kotlin, мультиплатформенная)
**Пакет:** `fleet.util`
**Комментарий в коде:** *«copied from Kotlin/Native stdlib»*

**Конструкторы:**
- `BitSet(size: Int = ELEMENT_SIZE)` — базовый, создаёт внутренний `LongArray`
- `BitSet(length: Int, initializer: (Int) -> Boolean)` — с инициализатором
- `BitSet(length: Int, bits: LongArray)` — из массива Long

**Свойства:**
- `size: Int` (публичное, автоматически расширяется)
- `lastTrueIndex: Int` (индекс последнего установленного бита, -1 если пуст)
- `isEmpty: Boolean`

**Методы доступа:**
- `operator get(index: Int)` -> `Boolean`
- `operator set(index: Int, value: Boolean)`

**Установка/сброс:**
- `set(from: Int, to: Int, value: Boolean)` — диапазон [from, to)
- `set(range: IntRange, value: Boolean)`
- `clear(index: Int)`, `clear(from: Int, to: Int)`, `clear(range: IntRange)`, `clear()` (все биты)

**Инверсия:**
- `flip(index: Int)`, `flip(from: Int, to: Int)`, `flip(range: IntRange)`

**Навигация:**
- `nextSetBit(startIndex: Int)` -> `Int` (-1 если не найден)
- `nextClearBit(startIndex: Int)` -> `Int`
- `previousBit(startIndex: Int, lookFor: Boolean)` -> `Int`
- `previousSetBit(startIndex: Int)` -> `Int`
- `previousClearBit(startIndex: Int)` -> `Int`

**Побитовые операции (in-place):**
- `and(another: BitSet)`, `or(another: BitSet)`, `xor(another: BitSet)`, `andNot(another: BitSet)`

**Прочее:**
- `intersects(another: BitSet)` -> `Boolean`
- `toLongArray()` -> `LongArray`
- `toString()`, `hashCode()`, `equals(other: Any?)`

**Внутреннее хранилище:** `LongArray` (64 бита на элемент, `ELEMENT_SIZE = 64`). Автоматически расширяется через `ensureCapacity()` с `copyOf()`. Неиспользуемые биты в конце обнуляются через `clearUnusedTail()`.

**Контекст:** Производная копия `kotlin.native.BitSet` из Kotlin/Native stdlib, размещённая в модуле `fleet/util/core` (common-main). Отличие от оригинала: добавлен конструктор `BitSet(length: Int, bits: LongArray)`, отсутствующий в текущем `kotlin.native.BitSet`. Используется в Fleet для мультиплатформенной совместимости вместо `java.util.BitSet`. Близкородственная копия той же K/N-основы есть в `platform/util/diff/` IntelliJ (секция 2 данного каталога), но с локальными отличиями: `diff/BitSet.kt` добавляет `@ApiStatus.Internal`, метод `cardinality()` и правку проверки диапазона (`range.endInclusive < -1` вместо `< 0`). Отсутствуют (в обеих копиях): `copy()`, `valueOf()`, итерация (`Iterable`/`Iterator`). Тот же набор ограничений, что и у оригинала из Kotlin/Native.

---

### 37. Прочее (grid, jps)

**grid/impl** (1 файл)

#### grid/impl/src/run/ui/HiddenColumnsSelectionHolder.java

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet;`
**Методы:** `new BitSet()`, `.set(int)`, `.clear(int)`, `.get(int)`, `.stream()`, `.toLongArray()`, `BitSet.valueOf(long[])`, `.clear()`
**Контекст:** Хранитель выделения скрытых столбцов в DataGrid (таблица данных базы данных). `BitSet` хранит множество model-индексов скрытых столбцов, которые выделены пользователем. `columnHidden()` устанавливает бит, `columnShown()` сбрасывает, `contains()` проверяет. Метод `selectedModelIndices()` использует `stream().toArray()` для получения массива индексов. Метод `copy()` клонирует через `BitSet.valueOf(toLongArray())`. Метод `reset()` сбрасывает все биты через `clear()`. Паттерн: BitSet как множество целочисленных индексов.

---

**jps/jps-builders** (1 файл)

#### jps/jps-builders/src/org/jetbrains/jps/incremental/IncProjectBuilder.java

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet;`
**Методы:** `new BitSet()`, `.or(BitSet)`, `.set(int)`, `.cardinality()`
**Контекст:** Инкрементальный билдер проекта JPS (Java Project Structure). `BitSet` используется для вычисления приоритетов задач сборки на основе **транзитивных зависимостей**. `.cardinality()` вычисляет `task.myDepsScore` — количество транзитивно зависимых задач. В `HashMap<BuildChunkTask, BitSet> chunkToTransitive` каждая задача ассоциируется с `BitSet`, хранящим индексы всех транзитивно зависимых задач. Алгоритм: обход задач в обратном порядке, для каждой задачи собираются `BitSet`-ы прямых зависимых, объединяются через `.or()`, добавляется индекс прямого зависимого через `.set(myIndex)`. Результат — граф транзитивных зависимостей, используемый для приоритизации. Паттерн: BitSet как множество индексов узлов графа зависимостей.

---

### 38. Сгенерированные лексеры: JFlex

#### python/python-parser/gen/com/jetbrains/python/lexer/_PythonLexer.java

**BitSet type:** `java.util.BitSet`
**Import:** `import java.util.BitSet;`
**Методы:** `new BitSet(int)`, `.set(int, boolean)`, `.get(int)`, `.clear(int)`, `.size()`
**Контекст:** Лексер Python, сгенерированный JFlex. `BitSet zzFin` используется для обратного DFA (deterministic finite automaton) в lookahead-выражениях. Для каждого lookahead-правила JFlex генерирует блок, в котором: (1) инициализирует `zzFin = new BitSet(zzBufferL.length()+1)` при необходимости расширения, (2) прямой проход: `zzFinL.set(zzFPos, (zzAttrL[zzFState] & 1) == 1)` — помечает принимающие позиции, (3) очистка хвоста: `zzFinL.clear(zzFPos++)`, (4) обратный проход: `while (!zzFinL.get(zzFPos) || ...)` — ищет последнюю принимающую позицию. Паттерн идентичен другим JFlex-лексерам (Markdown, RegExp и т.д.). В файле 6 блоков lookahead, каждый использует тот же `zzFin`.

---

### 39. Не-source файлы (API dumps, аннотации, тестовые данные)

14 файлов, не являющихся исходным кодом `.java`/`.kt`, но содержащих упоминания `BitSet`. Обнаружены расширенным поиском `grep -rl --exclude-dir=.git --binary-files=without-match "BitSet"`. Сгруппированы по типу артефакта (API dumps, JDK-файлы, аннотации, тестовые данные), а не по подсистемам: для не-source файлов группировка по типу артефакта более информативна, чем по подсистемам.

#### API dumps (7 файлов)

Файлы фиксации публичного API подсистем. Содержат сигнатуры методов/классов с `BitSet` в параметрах или возвращаемых типах.

- **`platform/util/concurrency/api-dump-unreviewed.txt`** — API `ConcurrentBitSet` (`create()`, `readFrom(DataInputStream)`) и `ConcurrentThreeStateBitSet`.
- **`platform/analysis-impl/api-dump-unreviewed.txt`** — поле `myReached: java.util.BitSet`, метод `computeUnreachable(java.util.BitSet)`.
- **`platform/diff-impl/api-dump.txt`** — `getSelectedLines(Editor): BitSet`, `isSelectedByLine(BitSet, int, int)`, `getRangesForLines(BitSet)`, `rollbackChanges(BitSet)`.
- **`platform/diff-impl/api-dump-experimental.txt`** — конструктор с параметрами `(int, int, BitSet, BitSet)`.
- **`platform/vcs-impl/api-dump.txt`** — `moveToChangelist(BitSet, LocalChangeList)`, `setExcludedFromCommit(BitSet, boolean)`, `setPartiallyExcludedFromCommit(BitSet, Side, boolean)`.
- **`platform/vcs-log/graph/api-dump.txt`** — класс `BitSetFlags`.
- **`plugins/git4idea/api-dump-unreviewed.txt`** — `getRangesForLines(BitSet)`, `rollbackChanges(BitSet)`.

---

#### JDK API version files (4 файла)

Списки API, добавленных в конкретных версиях JDK. Используются инспекцией совместимости с целевым JDK.

- **`java/java-analysis-api/resources/com/intellij/openapi/module/api1.4.txt`** — `BitSet#flip(int)`, `BitSet#flip(int, int)`, `BitSet#set(int, boolean)`, `BitSet#set(int, int)`, `BitSet#set(int, int, boolean)`.
- **`java/java-analysis-api/resources/com/intellij/openapi/module/api1.7.txt`** — `BitSet#valueOf(long[])`, `BitSet#valueOf(LongBuffer)`, `BitSet#valueOf(byte[])`, `BitSet#valueOf(ByteBuffer)`, `BitSet#toByteArray()`, `BitSet#toLongArray()`.
- **`java/java-analysis-api/resources/com/intellij/openapi/module/api1.8.txt`** — `BitSet#stream()`.
- **`java/java-analysis-api/resources/com/intellij/openapi/module/api22.txt`** — `ImmutableBitSetPredicate#of(BitSet)`.

---

#### JDK аннотации (1 файл)

- **`java/jdkAnnotations/java/util/annotations.xml`** — аннотации nullability и контрактов для методов `java.util.BitSet` (`get`, `intersects`, `isEmpty`, `toByteArray`, `toLongArray`).

---

#### Тестовые данные (2 файла)

- **`java/java-tests/testData/codeInsight/externalJavadoc/packageSummary/util/page.html`** — HTML-страница с ссылкой на `java.util.BitSet` в таблице классов пакета `java.util`. Тестовые данные для рендеринга Javadoc.
- **`xml/tests/testData/psi/old/html/ParserStackOverflow2.html`** — HTML-страница, упоминающая `AbstractBitSetEvent`. `grep`-hit по подстроке: файл не содержит прямой ссылки на тип `BitSet`, только имя класса `AbstractBitSetEvent` в HTML-ссылке. Тестовые данные для PSI-парсера HTML.

---

## Полнота

### Верификация

```
$ grep -rl "BitSet" --include="*.java" --include="*.kt" /Users/dmitry.nekrasov/dev/repos/intellij-community | wc -l
211

$ grep -rl --exclude-dir=.git --binary-files=without-match "BitSet" /Users/dmitry.nekrasov/dev/repos/intellij-community | wc -l
225
```

Каталог покрывает все 225 файлов (211 исходных `.java`/`.kt` + 14 не-source). Из 211 исходных файлов:
- **163 файла с реальным использованием API** java.util.BitSet или кастомных реализаций
- **~15 сгенерированных файлов** (12 Thrift, 3 JFlex) — документированы в condensed формате
- **~29 тестовых файлов** — документированы с пометкой «(тест)»
- **4 false positive** — отмечены в каталоге:
  - `ClassElement.java` — `BIT_SET` в именах `TokenSet`, не `java.util.BitSet`
  - `CollectionsMustHaveInitialCapacityInspection.java` — строковый FQN `"java.util.BitSet"` в списке отслеживаемых классов
  - `CollectionsMustHaveInitialCapacityInspectionTest.java` — строковые Java-сниппеты с `BitSet` в тестовых данных
  - `StreamExCallChecker.kt` — строковая константа `"toBitSet"`

Из 14 не-source файлов (секция 39):
- **7 API dumps** (`.txt`) — сигнатуры публичного API с `BitSet` в параметрах/типах
- **4 JDK API version files** (`.txt`) — списки методов `BitSet`, добавленных в JDK 1.4/1.7/1.8/22
- **1 JDK annotations file** (`.xml`) — аннотации для методов `java.util.BitSet`
- **2 тестовых HTML-файла** — тестовые данные, упоминающие `BitSet` / `AbstractBitSetEvent`

### Используемые типы BitSet

Предварительная сводка для навигации. Подсчёт: `rg -l 'TypeName'` — включает файлы с определением типа, его использованием, тестами и non-source (API dumps). Точные механические подсчёты с разбивкой будут выполнены в шаге 3c.

| Тип | Файлов |
|-----|--------|
| `java.util.BitSet` | ~155 |
| `com.intellij.util.containers.ConcurrentBitSet` | 19 |
| `com.intellij.util.containers.ConcurrentThreeStateBitSet` | 4 |
| `com.intellij.util.diff.BitSet` (custom Kotlin) | 11 |
| `com.intellij.vcs.log.graph.utils.impl.BitSetFlags` | 16 |
| `com.intellij.vcs.log.graph.utils.UnsignedBitSet` | 9 |
| `com.intellij.platform.syntax.impl.util.MutableBitSet` | 3 |
| `com.intellij.platform.syntax.impl.util.BitSet` (immutable) | 2 |
| `com.intellij.util.indexing.containers.BitSetAsRAIntContainer` | 4 |
| `com.intellij.util.indexing.containers.IdBitSet` | 6 |
| `mslinks.data.BitSet32` | 4 |
| `fleet.util.BitSet` (custom Kotlin) | 1 |
| `com.intellij.util.text.matching.BitSet` (typealias) | 2 |
