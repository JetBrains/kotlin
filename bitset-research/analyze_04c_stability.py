#!/usr/bin/env python3
"""
Step 4c stability analysis: leave-one-out and incremental convergence.

Parses step-04b-raw.tsv, computes per-repo method frequencies for use-site files,
then performs:
  1. Leave-one-out: remove each repo, check top-10 membership and top-5 order changes
  2. Incremental addition: add repos by descending use-count, track convergence

Output: markdown tables ready for step-04c-analysis.md
"""

import csv
from collections import defaultdict
from pathlib import Path

TSV = Path(__file__).parent / "step-04b-raw.tsv"


def parse_tsv():
    """Returns dict: repo -> list of method-sets (one set per use-file)."""
    repo_files = defaultdict(list)
    with open(TSV, newline="", encoding="utf-8") as f:
        reader = csv.DictReader(f, delimiter="\t")
        for row in reader:
            if row["cls"] != "use":
                continue
            methods_str = row.get("methods", "").strip()
            if methods_str:
                methods = {m.strip() for m in methods_str.split(";")}
            else:
                methods = set()
            repo_files[row["repo"]].append(methods)
    return repo_files


def compute_freq(repo_files, exclude_repo=None):
    """Compute method -> file-count across all repos except exclude_repo."""
    freq = defaultdict(int)
    for repo, file_methods in repo_files.items():
        if repo == exclude_repo:
            continue
        for methods in file_methods:
            for m in methods:
                freq[m] += 1
    return freq


def top_n(freq, n):
    """Return top-n methods sorted by count desc, then alphabetically."""
    return sorted(freq.items(), key=lambda x: (-x[1], x[0]))[:n]


def top_names(freq, n):
    return [m for m, _ in top_n(freq, n)]


def main():
    repo_files = parse_tsv()

    # Count use-files per repo
    repo_counts = {r: len(files) for r, files in repo_files.items()}
    total_use = sum(repo_counts.values())

    print(f"Total use-site files in TSV: {total_use}")
    print(f"Repos with use-site files: {sum(1 for c in repo_counts.values() if c > 0)}")
    print()

    # Global frequencies
    global_freq = compute_freq(repo_files)
    global_top10 = top_n(global_freq, 10)
    global_top10_names = set(top_names(global_freq, 10))
    global_top5_order = top_names(global_freq, 5)

    print("### Глобальная таблица (TSV, 417 use-files)")
    print()
    print("| Ранг | Метод | Файлов |")
    print("|---:|---|---:|")
    for i, (m, c) in enumerate(top_n(global_freq, 15), 1):
        print(f"| {i} | `{m}` | {c} |")
    print()

    # =========================================================================
    # 1. LEAVE-ONE-OUT
    # =========================================================================
    print("## 3.1 Leave-one-out analysis")
    print()
    print("Для каждого репозитория с ≥1 use-site файлом: убираем репозиторий, пересчитываем глобальные частоты, проверяем два триггера нестабильности.")
    print()
    print("| Repo | use | Top-10 membership Δ | Top-5 order Δ |")
    print("|---|---:|---|---|")

    membership_triggers = 0
    order_triggers = 0
    repos_sorted = sorted(repo_counts.items(), key=lambda x: -x[1])

    for repo, count in repos_sorted:
        if count == 0:
            continue
        freq_without = compute_freq(repo_files, exclude_repo=repo)
        t10_without = set(top_names(freq_without, 10))
        t5_without = top_names(freq_without, 5)

        membership_delta = ""
        if t10_without != global_top10_names:
            entered = t10_without - global_top10_names
            left = global_top10_names - t10_without
            parts = []
            if left:
                parts.append("−" + ", ".join(f"`{m}`" for m in sorted(left)))
            if entered:
                parts.append("+" + ", ".join(f"`{m}`" for m in sorted(entered)))
            membership_delta = "; ".join(parts)
            membership_triggers += 1
        else:
            membership_delta = "—"

        order_delta = ""
        if t5_without != global_top5_order:
            order_delta = " → ".join(f"`{m}`" for m in t5_without)
            order_triggers += 1
        else:
            order_delta = "—"

        print(f"| {repo} | {count} | {membership_delta} | {order_delta} |")

    print()
    print(f"**Итог:** membership trigger fired for {membership_triggers} repo(s); order trigger fired for {order_triggers} repo(s).")
    print()

    # =========================================================================
    # 2. INCREMENTAL ADDITION
    # =========================================================================
    print("## 3.2 Incremental convergence")
    print()
    print("Репозитории добавляются по убыванию числа use-site файлов. После каждого добавления фиксируются top-10 и top-5.")
    print()
    print("| # | Repo | use | Cumul | Top-5 order | Top-10 (new entries vs previous step) |")
    print("|---:|---|---:|---:|---|---|")

    cumulative_freq = defaultdict(int)
    prev_top10_names = set()
    prev_top5_order = []
    stabilized_at = None

    for step, (repo, count) in enumerate(repos_sorted, 1):
        if count == 0:
            continue
        # Add this repo's files
        for methods in repo_files[repo]:
            for m in methods:
                cumulative_freq[m] += 1

        cumul_total = sum(1 for r, c in repos_sorted[:step] if c > 0)
        cumul_files = sum(c for r, c in repos_sorted[:step])

        cur_top10_names = set(top_names(cumulative_freq, 10))
        cur_top5_order = top_names(cumulative_freq, 5)

        top5_str = ", ".join(f"`{m}`" for m in cur_top5_order)

        new_in_top10 = cur_top10_names - prev_top10_names
        if prev_top10_names:
            if new_in_top10:
                t10_delta = "+" + ", ".join(f"`{m}`" for m in sorted(new_in_top10))
            else:
                t10_delta = "—"
        else:
            t10_delta = "(initial)"

        # Track last change point; stabilization = first step after last change
        if step > 1 and (cur_top10_names != prev_top10_names or
                         cur_top5_order != prev_top5_order):
            last_change_step = step
        if step == 1:
            last_change_step = 1

        print(f"| {step} | {repo} | {count} | {cumul_files} | {top5_str} | {t10_delta} |")

        prev_top10_names = cur_top10_names
        prev_top5_order = cur_top5_order

    stabilized_at = last_change_step + 1 if last_change_step < len([r for r, c in repos_sorted if c > 0]) else None
    print()
    if stabilized_at:
        active_repos = [(r, c) for r, c in repos_sorted if c > 0]
        stab_repo = active_repos[stabilized_at - 1][0]
        stab_files = sum(c for r, c in active_repos[:stabilized_at])
        print(f"**Точка стабилизации:** шаг {stabilized_at} (после добавления `{stab_repo}`, cumulative {stab_files} use-files). С этого момента ни top-10 membership, ни top-5 order не меняются.")
    else:
        # Find partial stabilization
        # Check from which step top-5 order stops changing
        cumulative_freq2 = defaultdict(int)
        prev_t5 = []
        t5_stable_from = None
        prev_t10 = set()
        t10_stable_from = None
        for step, (repo, count) in enumerate(repos_sorted, 1):
            if count == 0:
                continue
            for methods in repo_files[repo]:
                for m in methods:
                    cumulative_freq2[m] += 1
            cur_t5 = top_names(cumulative_freq2, 5)
            cur_t10 = set(top_names(cumulative_freq2, 10))
            if cur_t5 == prev_t5 and t5_stable_from is None and step > 1:
                t5_stable_from = step
            elif cur_t5 != prev_t5:
                t5_stable_from = None
            if cur_t10 == prev_t10 and t10_stable_from is None and step > 1:
                t10_stable_from = step
            elif cur_t10 != prev_t10:
                t10_stable_from = None
            prev_t5 = cur_t5
            prev_t10 = cur_t10

        parts = []
        if t5_stable_from:
            r = repos_sorted[t5_stable_from - 1][0]
            parts.append(f"top-5 order стабилен с шага {t5_stable_from} (`{r}`)")
        if t10_stable_from:
            r = repos_sorted[t10_stable_from - 1][0]
            parts.append(f"top-10 membership стабилен с шага {t10_stable_from} (`{r}`)")
        if parts:
            print(f"**Частичная стабилизация:** {'; '.join(parts)}.")
        else:
            print("**Стабилизация не достигнута.**")
    print()

    # =========================================================================
    # 3. SUMMARY
    # =========================================================================
    print("## 3.3 Вывод")
    print()
    print("| Критерий | Результат |")
    print("|---|---|")

    # Check full LOO stability
    loo_membership_stable = membership_triggers == 0
    loo_order_stable = order_triggers == 0
    print(f"| LOO: top-10 membership stable | {'да' if loo_membership_stable else f'нет ({membership_triggers} repo(s) trigger)'} |")
    print(f"| LOO: top-5 order stable | {'да' if loo_order_stable else f'нет ({order_triggers} repo(s) trigger)'} |")
    if stabilized_at:
        print(f"| Incremental: full stabilization | шаг {stabilized_at} |")
    else:
        print(f"| Incremental: full stabilization | не достигнута |")
    print()


if __name__ == "__main__":
    main()
