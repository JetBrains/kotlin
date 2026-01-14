# Claude Code Setup Guide

> **Note:** This guide is for developers setting up Claude Code. AI agents should ignore this file.
>

## Installation

1. **CLI:** Check the official https://code.claude.com/docs/en/setup doc

2. **IntelliJ Plugin:** Install "Claude Code [Beta]" from the plugin marketplace

3. **MCP Server:** Settings | Tools | MCP Server → Click "Auto-Configure for Claude Code"

## Verification

- `/mcp` Claude command — Verify JetBrains MCP is enabled
  - If missing, check `.claude/settings.local.json` file for `disabledMcpjsonServers` or `deniedMcpServers`
- `/model` Claude command — Ensure Opus 4.5 is selected (Sonnet 4.5 is less capable)
- The configured MCP server might have different names, but you might want to rename it to `jetbrains` for consistency

## Thinking Mode

Thinking is **on by default** in CC >2.0.70 and it's discouraged to turn it off. 

Previously claude required prompting it to "think harder" or "ultrathink" – it's now not required

## Local Preferences

`../.claude/local.md` file can be used to store local preferences