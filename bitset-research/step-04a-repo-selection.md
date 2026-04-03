# Шаг 4a. Отбор репозиториев и методология

**Резюме.** Отобраны 23 внешних open-source репозитория с подтверждённым нетривиальным использованием `java.util.BitSet` (≥3 файлов с упоминанием в code search). Репозитории покрывают 11 доменов: compilers/JVM, search/indexing, Android, JVM core libraries, data processing, static analysis/code quality, database engines, networking, collections/data structures, web frameworks, ORM. Суммарно проект охватывает более 420 тыс. звёзд на GitHub; медианный репозиторий — 8,5k звёзд.

**Входные данные:** seed-список из [`bitset-research-plan.md`](bitset-research-plan.md), раздел «Шаг 4a».

## 1. Методология

### 1.1. Инструменты и ограничения API

- **Проверка плотности:** `gh search code "java.util.BitSet" --repo=OWNER/REPO --json path --jq 'length'` (GitHub Code Search endpoint, аутентифицированный режим, консервативный бюджет ≤9 запросов/мин).
- **Метаданные репозиториев:** `gh api repos/OWNER/REPO` (Core API, ≤5000 запросов/час — нет узкого места).
- **Порог включения:** ≥3 файлов с упоминанием `java.util.BitSet` в результатах code search (прокси-метрика для import; подробнее — ограничение 2).

**Caveat:** В использованных вызовах `gh search code` без параметра `--limit` выводились только первые 30 результатов (значение по умолчанию). Для репозиториев, показывающих ровно 30 файлов, реальное число может быть выше. В таблице ниже такие значения помечены как `≥30`.

### 1.2. Стратегия поиска

1. **Seed-верификация** (из плана исследования): 11 репозиториев проверены на плотность. 7 прошли порог, 4 отсеяны.
2. **Расширенный поиск — broad:** `gh search code "java.util.BitSet" --language=java --limit=100 --json repository --jq '.[].repository.fullName' | sort -u` для обнаружения дополнительных репозиториев. Результат: дедуплицированный список ~85 уникальных имён репозиториев (из первых 100 code results, отсортированных по best match). Воспроизводимый след broad-phase сводится к этому списку имён: `sort -u` отбрасывает кратность, метаданные (stars, fork-статус) не запрашивались, per-repo верификация (число файлов ≥3) не проводилась. Broad-phase использовался как эвристический discovery-шаг; аудируемых выводов об отдельных репозиториях из него не извлекалось. Вывод ограничен top-ranked подмножеством, а не всей индексной базой GitHub Code Search. Аналогичный запрос с `--language=kotlin` не проводился: `androidx/androidx` уже входил в seed-список и обеспечивал покрытие Android-экосистемы, а `java.util.BitSet` — Java API, покрываемый преимущественно Java-кодом. Наличие других крупных Kotlin-first кандидатов данной выборкой не проверялось.
3. **Расширенный поиск — curated:** на основе доменного знания составлен список кандидатов из крупных OSS-проектов, вероятно использующих BitSet (базы данных, ORM, web frameworks, collections, data pipelines). 21 кандидат проверен, 16 прошли порог.
4. **Итого:** 23 репозитория из 32 проверенных кандидатов.

### 1.3. Rate limit management

Запросы к Code Search API выполнялись пакетами не более 9 с паузой ≥60 секунд между пакетами (аутентифицированный `gh`-сеанс, консервативный бюджет 9 запросов/мин; документация GitHub Search code на 2026-04-03 содержит расхождение: 9 и 10 запросов/мин в разных секциях). Всего выполнено 4 пакета (≈33 запроса code search + метаданные через Core API).

### 1.4. Список исключений

Репозитории, проанализированные в Шаге 3, исключены из выборки:
- `JetBrains/kotlin`
- `JetBrains/intellij-community`
- `JetBrains/Grammar-Kit`
- `JetBrains/markdown`

## 2. Seed-репозитории: верификация

| # | Repo | Файлов | Статус | Причина отсева |
|---|---|---:|---|---|
| 1 | `antlr/antlr4` | 14 | **Принят** | — |
| 2 | `oracle/graal` | ≥30 | **Принят** | — |
| 3 | `raphw/byte-buddy` | 0 | Отсеян | Нет упоминаний `java.util.BitSet` |
| 4 | `ow2-asm/asm` | 0 | Отсеян | Нет упоминаний `java.util.BitSet` |
| 5 | `apache/lucene` | ≥30 | **Принят** | — |
| 6 | `androidx/androidx` | 7 | **Принят** | — |
| 7 | `google/guava` | 20 | **Принят** | — |
| 8 | `gradle/gradle` | 2 | Отсеян | Ниже порога (≥3) |
| 9 | `apache/spark` | 4 | **Принят** | — |
| 10 | `jgrapht/jgrapht` | 0 | Отсеян | Нет упоминаний `java.util.BitSet` |
| 11 | `spotbugs/spotbugs` | ≥30 | **Принят** | — |

**Результат:** 7 из 11 seed-репозиториев прошли верификацию.

## 3. Расширенный поиск: дополнительные кандидаты

| # | Repo | Файлов | Статус | Источник кандидата |
|---|---|---:|---|---|
| 12 | `apache/calcite` | 22 | **Принят** | Curated: SQL query optimizer |
| 13 | `h2database/h2database` | 23 | **Принят** | Curated: embedded database |
| 14 | `checkstyle/checkstyle` | ≥30 | **Принят** | Curated: Java code quality |
| 15 | `pmd/pmd` | 3 | **Принят** | Curated: static analysis |
| 16 | `apache/flink` | ≥30 | **Принят** | Curated: stream processing |
| 17 | `netty/netty` | 5 | **Принят** | Curated: networking |
| 18 | `eclipse-collections/eclipse-collections` | 10 | **Принят** | Curated: collections framework |
| 19 | `apache/druid` | 28 | **Принят** | Curated: analytics DB |
| 20 | `apache/hive` | ≥30 | **Принят** | Curated: data warehouse |
| 21 | `spring-projects/spring-framework` | 4 | **Принят** | Curated: web framework |
| 22 | `hibernate/hibernate-orm` | ≥30 | **Принят** | Curated: ORM |
| 23 | `RoaringBitmap/RoaringBitmap` | 18 | **Принят** | Curated: bitmap data structures |
| 24 | `apache/commons-lang` | 4 | **Принят** | Curated: JVM utilities |
| 25 | `apache/beam` | 11 | **Принят** | Curated: data pipeline |
| 26 | `elastic/elasticsearch` | ≥30 | **Принят** | Curated: search engine |
| 27 | `apache/cassandra` | 14 | **Принят** | Curated: distributed DB |
| — | `eclipse-jdt/eclipse.jdt.core` | 1 | Отсеян | Ниже порога |
| — | `jacoco/jacoco` | 2 | Отсеян | Ниже порога |
| — | `eclipse-openj9/openj9` | 2 | Отсеян | Ниже порога |
| — | `mybatis/mybatis-3` | 0 | Отсеян | Нет файлов |
| — | `apache/kafka` | 0 | Отсеян | Нет файлов |

**Результат:** 16 из 21 дополнительного кандидата прошли верификацию.

## 4. Финальный список репозиториев

Колонка «Обоснование включения» отражает значимость репозитория и его доменную релевантность — характеристики, подтверждаемые метаданными и публичным описанием проекта. Конкретные сценарии использования BitSet внутри каждого репозитория будут извлечены и задокументированы на шаге 4b.

| # | Репозиторий | Stars | Язык | Домен | Файлов | Обоснование включения |
|---|---|---:|---|---|---:|---|
| 1 | [`antlr/antlr4`](https://github.com/antlr/antlr4) | 18 807 | Java | Compilers / parsers / JVM | 14 | Ведущий парсер-генератор с широким промышленным применением |
| 2 | [`oracle/graal`](https://github.com/oracle/graal) | 21 538 | Java | Compilers / parsers / JVM | ≥30 | JIT-компилятор и нативный runtime от Oracle |
| 3 | [`apache/lucene`](https://github.com/apache/lucene) | 3 397 | Java | Search / indexing | ≥30 | Эталонный поисковый движок Apache |
| 4 | [`androidx/androidx`](https://github.com/androidx/androidx) | 5 944 | Kotlin | Android ecosystem | 7 | Основная библиотека Android Jetpack; единственный крупный Kotlin-first репозиторий в выборке |
| 5 | [`google/guava`](https://github.com/google/guava) | 51 517 | Java | JVM core libraries / utilities | 20 | Широко используемая утилитарная библиотека от Google |
| 6 | [`apache/spark`](https://github.com/apache/spark) | 43 079 | Scala | Data processing / pipeline | 4 | Ведущий фреймворк распределённых вычислений |
| 7 | [`spotbugs/spotbugs`](https://github.com/spotbugs/spotbugs) | 3 851 | Java | Static analysis / code quality | ≥30 | Статический анализатор Java-байткода; наследник FindBugs |
| 8 | [`apache/calcite`](https://github.com/apache/calcite) | 5 103 | Java | Database engines | 22 | SQL-оптимизатор и query framework Apache |
| 9 | [`h2database/h2database`](https://github.com/h2database/h2database) | 4 614 | Java | Database engines | 23 | Встроенная SQL СУБД на Java |
| 10 | [`checkstyle/checkstyle`](https://github.com/checkstyle/checkstyle) | 8 912 | Java | Static analysis / code quality | ≥30 | Линтер Java-кода с обширной базой правил |
| 11 | [`pmd/pmd`](https://github.com/pmd/pmd) | 5 368 | Java | Static analysis / code quality | 3 | Мультиязычный статический анализатор; минимальный порог, но важный домен |
| 12 | [`apache/flink`](https://github.com/apache/flink) | 25 910 | Java | Data processing / pipeline | ≥30 | Ведущий фреймворк потоковой обработки данных |
| 13 | [`netty/netty`](https://github.com/netty/netty) | 34 901 | Java | Networking | 5 | Ведущий сетевой фреймворк; единственный представитель домена networking |
| 14 | [`eclipse-collections/eclipse-collections`](https://github.com/eclipse-collections/eclipse-collections) | 2 625 | Java | Collections / data structures | 10 | Альтернативный collections framework (Eclipse Foundation) |
| 15 | [`apache/druid`](https://github.com/apache/druid) | 13 969 | Java | Database engines | 28 | OLAP-база для аналитики реального времени |
| 16 | [`apache/hive`](https://github.com/apache/hive) | 6 016 | Java | Data processing / pipeline | ≥30 | SQL-on-Hadoop экосистема Apache |
| 17 | [`spring-projects/spring-framework`](https://github.com/spring-projects/spring-framework) | 59 809 | Java | Web frameworks | 4 | Ведущий Java web framework; единственный представитель домена |
| 18 | [`hibernate/hibernate-orm`](https://github.com/hibernate/hibernate-orm) | 6 464 | Java | ORM | ≥30 | Ведущий Java ORM; единственный представитель домена |
| 19 | [`RoaringBitmap/RoaringBitmap`](https://github.com/RoaringBitmap/RoaringBitmap) | 3 847 | Java | Collections / data structures | 18 | Специализированная сжатая bitmap-структура; тематически наиболее близок к BitSet |
| 20 | [`apache/commons-lang`](https://github.com/apache/commons-lang) | 2 955 | Java | JVM core libraries / utilities | 4 | Базовая JVM-утилита из экосистемы Apache Commons |
| 21 | [`apache/beam`](https://github.com/apache/beam) | 8 535 | Java | Data processing / pipeline | 11 | Unified batch/stream модель обработки данных |
| 22 | [`elastic/elasticsearch`](https://github.com/elastic/elasticsearch) | 76 438 | Java | Search / indexing | ≥30 | Распределённый поисковый движок; крупнейший по аудитории проект в выборке |
| 23 | [`apache/cassandra`](https://github.com/apache/cassandra) | 9 681 | Java | Database engines | 14 | Распределённая NoSQL-СУБД Apache |

### Агрегатные показатели

| Метрика | Значение |
|---|---|
| Всего репозиториев | 23 |
| Репозитории с ≥30 файлов (default `--limit`) | 8 |
| Медиана звёзд | 8 535 |
| Мин. звёзд | 2 625 (`eclipse-collections`) |
| Макс. звёзд | 76 438 (`elasticsearch`) |
| Суммарно звёзд | ~423 000 |
| Kotlin-first репозиториев | 1 (`androidx`) |
| Scala-first репозиториев | 1 (`spark`) |
| Java-first репозиториев | 21 |

## 5. Покрытие доменов

| # | Домен | Репозитории | Кол-во |
|---|---|---|---:|
| 1 | Compilers / parsers / JVM | antlr4, graal | 2 |
| 2 | Search / indexing | lucene, elasticsearch | 2 |
| 3 | Android ecosystem | androidx | 1 |
| 4 | JVM core libraries / utilities | guava, commons-lang | 2 |
| 5 | Data processing / pipeline | spark, flink, beam, hive | 4 |
| 6 | Static analysis / code quality | spotbugs, pmd, checkstyle | 3 |
| 7 | Database engines | calcite, h2database, druid, cassandra | 4 |
| 8 | Networking | netty | 1 |
| 9 | Collections / data structures | eclipse-collections, RoaringBitmap | 2 |
| 10 | Web frameworks | spring-framework | 1 |
| 11 | ORM | hibernate-orm | 1 |

**Итого: 11 доменов** (требование: ≥5). Android-экосистема представлена (`androidx/androidx`).

## 6. Ограничения выборки

1. **Лимит `--limit=30` по умолчанию.** `gh search code` без явного `--limit` возвращает только первые 30 результатов. Для 8 из 23 репозиториев code search вернул ровно 30 результатов — реальное число файлов может быть значительно выше. На этапе 4b будет использован дополнительный поиск внутри этих репозиториев.

2. **Поиск по строке `java.util.BitSet` — прокси-метрика.**
   - *Механизм поиска:* GitHub Code Search [игнорирует точки и другие спецсимволы](https://docs.github.com/en/search-github/searching-on-github/searching-code) в запросе, поэтому `"java.util.BitSet"` фактически ищет файлы с токенами `java`, `util` и `BitSet`, а не literal-строку. На практике совместное вхождение этих токенов в Java/Kotlin-исходниках сильно коррелирует с реальным использованием `java.util.BitSet`, но формально запрос шире, чем exact-match.
   - *Завышение:* помимо import-строк, матчатся fully-qualified usage (`new java.util.BitSet()`), комментарии, Javadoc, тестовые строки, а также потенциально файлы, где токены встречаются в разных контекстах. Для пограничных репозиториев (pmd = 3, spring-framework = 4, commons-lang = 4, netty = 5) реальное число файлов с import может быть ниже заявленного.
   - *Занижение:* не покрывает wildcard-импорты (`import java.util.*`) и использование через обёртки/адаптеры без прямого упоминания `java.util.BitSet`. Kotlin aliased imports (`import java.util.BitSet as Alias`) покрываются, так как полное квалифицированное имя присутствует в строке импорта. Реальное использование шире.
   - *Верификация:* план исследования специфицирует `gh search code "java.util.BitSet"` как метод подтверждения; артефакт следует этой методологии. Точная import-верификация (подтверждение `import java.util.BitSet` в содержимом файлов) запланирована на шаге 4b; для пограничных репозиториев она приоритетна.

3. **Доминирование Java.** 21 из 23 репозиториев — Java-first. Kotlin-специфичные паттерны представлены только `androidx`. Это отражает реальную экосистему (Java BitSet — Java API), но ограничивает выводы о Kotlin-идиомах.

4. **Bias к крупным OSS-проектам.** Curated-подход ориентировался на заведомо крупные OSS-проекты; систематический фильтр `stars ≥100`, предписанный планом для broad-phase, фактически не применялся — broad-phase использовался только как эвристический discovery без per-repo верификации (см. п. 1.2). Как результат, выборка исключает малые проекты, embedded-разработку и enterprise-код. Дедуплицированный broad-search срез (~85 уникальных имён) не содержит метаданных для систематического отсева; per-repo проверка stars/fork-статуса для этих репозиториев не проводилась (см. п. 1.2), поэтому наличие пропущенных нетривиальных кандидатов не исключено.

5. **Snapshot in time.** Данные собраны 2026-04-03. GitHub Code Search индексирует default branch; изменения после этой даты не отражены.

6. **Отсутствие bytecode manipulation.** Seed-кандидаты `byte-buddy` и `asm` показали 0 файлов — возможно, используют wildcard imports или собственные битовые структуры. Домен не представлен.

7. **Расхождение REST API и web UI.** `gh search code` использует REST API `/search/code`, результаты которого могут расходиться с интерфейсом code search на github.com. Для пограничных репозиториев рекомендуется перепроверка через web UI на шаге 4b.

## 7. Отсеянные кандидаты

| Repo | Файлов | Причина |
|---|---:|---|
| `raphw/byte-buddy` | 0 | Нет упоминаний `java.util.BitSet` |
| `ow2-asm/asm` | 0 | Нет упоминаний `java.util.BitSet` |
| `jgrapht/jgrapht` | 0 | Нет упоминаний `java.util.BitSet` |
| `mybatis/mybatis-3` | 0 | Нет упоминаний `java.util.BitSet` |
| `apache/kafka` | 0 | Нет упоминаний `java.util.BitSet` |
| `gradle/gradle` | 2 | Ниже порога ≥3 |
| `eclipse-jdt/eclipse.jdt.core` | 1 | Ниже порога ≥3 |
| `jacoco/jacoco` | 2 | Ниже порога ≥3 |
| `eclipse-openj9/openj9` | 2 | Ниже порога ≥3 |
