/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

{
  "note": "May https://vega.github.io/vega/docs/ be with you",
  "$schema": "https://vega.github.io/schema/vega/v4.3.0.json",
  "description": "The Kotlin sources: highlight",
  "title": "The Kotlin sources: highlight",
  "width": 800,
  "height": 500,
  "padding": 5,
  "autosize": {"type": "pad", "resize": true},
  "signals": [
    {
      "name": "clear",
      "value": true,
      "on": [
        {"events": "mouseup[!event.item]", "update": "true", "force": true}
      ]
    },
    {
      "name": "shift",
      "value": false,
      "on": [
        {
          "events": "@legendSymbol:click, @legendLabel:click",
          "update": "event.shiftKey",
          "force": true
        }
      ]
    },
    {
      "name": "clicked",
      "value": null,
      "on": [
        {
          "events": "@legendSymbol:click, @legendLabel:click",
          "comment": "note: here `datum` is `selected` data set",
          "update": "{value: datum.value}",
          "force": true
        }
      ]
    },
    {
      "name": "branchShift",
      "value": false,
      "on": [
        {
          "events": "@branchLegendSymbol:click, @branchLegendLabel:click",
          "update": "event.shiftKey",
          "force": true
        }
      ]
    },
    {
      "name": "branchClicked",
      "value": null,
      "on": [
        {
          "events": "@branchLegendSymbol:click, @branchLegendLabel:click",
          "comment": "note: here `datum` is `selected` data set",
          "update": "{value: datum.value}",
          "force": true
        }
      ]
    },
    {
      "name": "brush",
      "value": 0,
      "on": [
        {"events": {"signal": "clear"}, "update": "clear ? [0, 0] : brush"},
        {"events": "@xaxis:mousedown", "update": "[x(), x()]"},
        {
          "events": "[@xaxis:mousedown, window:mouseup] > window:mousemove!",
          "update": "[brush[0], clamp(x(), 0, width)]"
        },
        {
          "events": {"signal": "delta"},
          "update": "clampRange([anchor[0] + delta, anchor[1] + delta], 0, width)"
        }
      ]
    },
    {
      "name": "anchor",
      "value": null,
      "on": [{"events": "@brush:mousedown", "update": "slice(brush)"}]
    },
    {
      "name": "xdown",
      "value": 0,
      "on": [{"events": "@brush:mousedown", "update": "x()"}]
    },
    {
      "name": "delta",
      "value": 0,
      "on": [
        {
          "events": "[@brush:mousedown, window:mouseup] > window:mousemove!",
          "update": "x() - xdown"
        }
      ]
    },
    {
      "name": "domain",
      "on": [
        {
          "events": {"signal": "brush"},
          "update": "span(brush) ? invert('x', brush) : null"
        }
      ]
    },
    {"name": "timestamp", "value": true, "bind": {"input": "checkbox"}}
  ],
  "data": [
    {
      "name": "table",
      "comment": "To test chart in VEGA editor https://vega.github.io/editor/#/ change `_values` to `values` and rename `url` property",
      "_values": {
        "comment": "same as `TestData highlight - vega.js`"
      },
      "url": {
        //"comment": "source index pattern",
        "index": "kotlin_ide_benchmarks*",
        //"comment": "it's a body of ES _search query to check query place it into `POST /kotlin_ide_benchmarks*/_search`",
        //"comment": "it uses Kibana specific %timefilter% for time frame selection",
        "body": {
          "size": 0,
          "query": {
            "bool": {
              "must": [
                {"range": {"buildTimestamp": {"%timefilter%": true}}},
                    {
                      "bool": {
                        "must_not": [
                           {"exists": {"field": "synthetic"}},
                           {"term": {"name.keyword": "open project kotlin"}}
                         ]
                       }
                    },
                    {
                      "bool": {
                        "should": [
                          {"term": {"benchmark.keyword": "kotlin project"}}
                        ]
                      }
                    }],
                  "filter": [
                    {"prefix": {"name": {"value": "highlighting"}}}
                  ],
                  "must_not": [
                    {"prefix": {
                      "name.keyword": {
                        "value": "highlighting empty profile"
                      }
                    }}
                ]
              }
            },
          "aggs": {
            "benchmark": {
              "terms": {
                "field": "benchmark.keyword",
                "size": 500
              },
              "aggs": {
                "name": {
                  "terms": {
                    "field": "name.keyword",
                    "size": 500
                  },
                  "aggs": {
                    "values": {
                      "auto_date_histogram": {
                          "buckets": 500,
                          "field": "buildTimestamp",
                          "minimum_interval": "hour"
                      },
                      "aggs": {
                        "buildId": {
                          "terms": {
                            "size": 1,
                            "field": "buildId"
                          }
                        },
                        "branch": {
                          "terms": {
                            "size": 1,
                            "field": "buildBranch.keyword"
                          }
                        },
                        "avgValue":{
                          "avg": {
                            "field": "metricValue"
                          }
                        },
                        "avgError":{
                          "avg": {
                            "field": "metricError"
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
      },
      "format": {"property": "aggregations"},
      "comment": "we need to have follow data: \"buildId\", \"metricName\", \"metricValue\" and \"metricError\"",
      "comment": "so it has to be array of {\"buildId\": \"...\", \"metricName\": \"...\", \"metricValue\": ..., \"metricError\": ...}",
      "transform": [
        {"type": "project", "fields": ["benchmark"]},
        {"type": "flatten", "fields": ["benchmark.buckets"], "as": ["benchmark_buckets"]},
        {"type": "project", "fields": ["benchmark_buckets.key", "benchmark_buckets.name"], "as": ["benchmark", "benchmark_buckets_name"]},
        {"type": "flatten", "fields": ["benchmark_buckets_name.buckets"], "as": ["name_buckets"]},
        {"type": "project", "fields": ["benchmark", "name_buckets.key", "name_buckets.values"], "as": ["benchmark", "name", "name_values"]},
        {"type": "flatten", "fields": ["name_values.buckets"], "as": ["name_values_buckets"]},
        {"type": "project", "fields": ["benchmark", "name", "name_values_buckets.key", "name_values_buckets.key_as_string", "name_values_buckets.avgError", "name_values_buckets.avgValue", "name_values_buckets.buildId.buckets", "name_values_buckets.branch.buckets"], "as": ["benchmark", "metricName", "buildTimestamp", "timestamp_value", "avgError", "avgValue", "buildId_buckets", "branch_buckets"]},
        {"type": "formula", "as": "metricError", "expr": "datum.avgError.value"},
        {"type": "formula", "as": "metricValue", "expr": "datum.avgValue.value"},
        {"type": "flatten", "fields": ["buildId_buckets"], "as": ["buildId_values"]},
        {"type": "flatten", "fields": ["branch_buckets"], "as": ["branch_values"]},
        {"type": "formula", "as": "buildId", "expr": "datum.buildId_values.key"},
        {"type": "formula", "as": "branch", "expr": "datum.branch_values.key"},
        {
          "type": "formula",
          "as": "timestamp",
          "expr": "timeFormat(toDate(datum.buildTimestamp), '%Y-%m-%d %H:%M')"
        },
        {
          "comment": "create `url` value that points to TC build",
          "type": "formula",
          "as": "url",
          "expr": "'https://buildserver.labs.intellij.net/buildConfiguration/Kotlin_Benchmarks_PluginPerformanceTests_IdeaPluginPerformanceTests/' + datum.buildId"
        },
        {"type": "collect","sort": {"field": "timestamp"}}
      ]
    },
    {
      "name": "selected",
      "on": [
        {"trigger": "clear", "remove": true},
        {"trigger": "!shift", "remove": true},
        {"trigger": "!shift && clicked", "insert": "clicked"},
        {"trigger": "shift && clicked", "toggle": "clicked"}
      ]
    },
    {
        "name": "selectedBranch",
        "on": [
         {"trigger": "clear", "remove": true},
         {"trigger": "!branchShift", "remove": true},
         {"trigger": "!branchShift && branchClicked", "insert": "branchClicked"},
         {"trigger": "branchShift && branchClicked", "toggle": "branchClicked"}
        ]
    }
  ],
  "axes": [
    {
      "scale": "x",
      "grid": true,
      "domain": false,
      "orient": "bottom",
      "labelAngle": -20,
      "labelAlign": "right",
      "title": {"signal": "timestamp ? 'timestamp' : 'buildId'"},
      "titlePadding": 10,
      "tickCount": 5,
      "encode": {
        "labels": {
          "interactive": true,
          "update": {"tooltip": {"signal": "datum.label"}}
        }
      }
    },
    {
      "scale": "y",
      "grid": true,
      "domain": false,
      "orient": "left",
      "titlePadding": 10,
      "title": "ms",
      "titleAnchor": "end",
      "titleAngle": 0
    }
  ],
  "scales": [
    {
      "name": "x",
      "type": "point",
      "range": "width",
      "domain": {"data": "table", "field": {"signal": "timestamp ? 'timestamp' : 'buildId'"}}
    },
    {
      "name": "y",
      "type": "linear",
      "range": "height",
      "nice": true,
      "zero": true,
      "domain": {"data": "table", "field": "metricValue"}
    },
    {
      "name": "color",
      "type": "ordinal",
      "range": "category",
      "domain": {"data": "table", "field": "metricName"}
    },
    {
      "name": "size",
      "type": "linear",
      "round": true,
      "nice": false,
      "zero": true,
      "domain": {"data": "table", "field": "metricError"},
      "range": [1, 100]
    },
    {
      "name": "branchColor",
      "type": "ordinal",
      "domain": {"data": "table", "field": "branch"},
      "range": "category"
    }
  ],
  "legends": [
    {
      "title": "Cases",
      "stroke": "color",
      "strokeColor": "#ccc",
      "padding": 8,
      "cornerRadius": 4,
      "symbolLimit": 50,
      "labelLimit": 300,
      "encode": {
        "symbols": {
          "name": "legendSymbol",
          "interactive": true,
          "update": {
            "fill": {"value": "transparent"},
            "strokeWidth": {"value": 2},
            "opacity": [
              {
                "comment": "here `datum` is `selected` data set",
                "test": "!length(data('selected')) || indata('selected', 'value', datum.value)",
                "value": 0.7
              },
              {"value": 0.15}
            ],
            "size": {"value": 64}
          }
        },
        "labels": {
          "name": "legendLabel",
          "interactive": true,
          "update": {
            "opacity": [
              {
                "comment": "here `datum` is `selected` data set",
                "test": "!length(data('selected')) || indata('selected', 'value', datum.value)",
                "value": 1
              },
              {"value": 0.25}
            ]
          }
        }
      }
    },
    {
      "title": "Branches",
      "stroke": "branchColor",
      "fill": "branchColor",
      "strokeColor": "#ccc",
      "padding": 8,
      "cornerRadius": 4,
      "symbolLimit": 50,
      "labelLimit": 300,
      "encode": {
        "symbols": {
          "name": "branchLegendSymbol",
          "interactive": true,
          "update": {
            "strokeWidth": {"value": 2},
            "opacity": [
              {
                "comment": "here `datum` is `selectedBranch` data set",
                "test": "!length(data('selectedBranch')) || indata('selectedBranch', 'value', datum.value)",
                "value": 0.7
              },
              {"value": 0.15}
            ],
            "size": {"value": 64}
          }
        },
        "labels": {
          "name": "branchLegendLabel",
          "interactive": true,
          "update": {
            "opacity": [
              {
                "comment": "here `datum` is `selectedBranch` data set",
                "test": "!length(data('selectedBranch')) || indata('selectedBranch', 'value', datum.value)",
                "value": 1
              },
              {"value": 0.25}
            ]
          }
        }
      }
    }
  ],
  "marks": [
    {
      "type": "group",
      "from": {
        "facet": {"name": "series", "data": "table", "groupby": "metricName"}
      },
      "marks": [
        {
          "type": "text",
          "from": {"data": "series"},
          "encode": {
            "update": {
              "x": {"scale": "x", "field": {"signal": "timestamp ? 'timestamp' : 'buildId'"}},
              "align": {"value": "center"},
              "y": {"value": -10},
              "angle": {"value": 90},
              "fill": {"value": "#000"},
              "text": [{"test": "datum.branch != 'master'", "field": "branch"}, {"value": ""}],
              "fontSize": {"value": 10},
              "font": {"value": "monospace"}
            }
          }
        },
        {
          "type": "rect",
          "from": {"data": "series"},
          "encode": {
            "update": {
              "x": {"scale": "x", "field": {"signal": "timestamp ? 'timestamp' : 'buildId'"}, "offset":-5},
              "x2": {"scale": "x", "field": {"signal": "timestamp ? 'timestamp' : 'buildId'"}, "offset": 5},
              "y": {"value": 0},
              "y2": {"signal": "height"},
              "fill": [{"test": "datum.branch != 'master'", "scale": "branchColor", "field": "branch"}, {"value": ""}],
              "opacity": [
                  {
                    "test": "(!domain || inrange(datum.branch, domain)) && (!length(data('selectedBranch')) || indata('selectedBranch', 'value', datum.branch))",
                    "value": 0.1
                  },
                  {"value": 0.01}
                ]
            }
          }
        },
        {
          "type": "line",
          "from": {"data": "series"},
          "encode": {
            "hover": {"opacity": {"value": 1}, "strokeWidth": {"value": 4}},
            "update": {
              "x": {"scale": "x", "field": {"signal": "timestamp ? 'timestamp' : 'buildId'"}},
              "y": {"scale": "y", "field": "metricValue"},
              "strokeWidth": {"value": 2},
              "opacity": [
                {
                  "test": "(!domain || inrange(datum.buildId, domain)) && (!length(data('selected')) || indata('selected', 'value', datum.metricName))",
                  "value": 0.7
                },
                {"value": 0.15}
              ],
              "stroke": [
                {
                  "test": "(!domain || inrange(datum.buildId, domain)) && (!length(data('selected')) || indata('selected', 'value', datum.metricName))",
                  "scale": "color",
                  "field": "metricName"
                },
                {"value": "#ccc"}
              ]
            }
          }
        },
        {
          "type": "symbol",
          "from": {"data": "series"},
          "encode": {
            "enter": {
              "fill": {"value": "#B00"},
              "size": [{ "test": "datum.hasError", "value": 250 }, {"value": 0}],
              "shape": {"value": "cross"},
              "angle": {"value": 45},
              "x": {"scale": "x", "field": {"signal": "timestamp ? 'timestamp' : 'buildId'"}},
              "y": {"scale": "y", "field": "metricValue"},
              "strokeWidth": {"value": 1},
              "opacity": [
                {
                  "test": "(!domain || inrange(datum.buildId, domain)) && datum.hasError && (!length(data('selected')) || indata('selected', 'value', datum.metricName))",
                  "value": 1
                },
                {"value": 0.15}
              ]
            },
            "update": {
              "opacity": [
                {
                  "test": "(!domain || inrange(datum.buildId, domain))  && datum.hasError && (!length(data('selected')) || indata('selected', 'value', datum.metricName))",
                  "value": 1
                },
                {"value": 0.15}
              ]
            }
          },
          "zindex": 1
        },
        {
          "type": "symbol",
          "from": {"data": "series"},
          "encode": {
            "enter": {
              "tooltip": {
                "signal": "datum.metricName + ': ' + datum.metricValue + ' ms'"
              },
              "href": {"field": "url"},
              "cursor": {"value": "pointer"},
              "size": {"scale": "size", "field": "metricError"},
              "x": {"scale": "x", "field": {"signal": "timestamp ? 'timestamp' : 'buildId'"}},
              "y": {"scale": "y", "field": "metricValue"},
              "strokeWidth": {"value": 1},
              "fill": {"scale": "color", "field": "metricName"}
            },
            "update": {
              "opacity": [
                {
                  "test": "(!domain || inrange(datum.buildId, domain)) && (!length(data('selected')) || indata('selected', 'value', datum.metricName))",
                  "value": 1
                },
                {"value": 0.15}
              ]
            }
          },
          "zindex": 2
        }
      ]
    }
  ]
}
